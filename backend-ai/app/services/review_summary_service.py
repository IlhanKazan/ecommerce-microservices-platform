"""Yorum özetleme servisi.

Akış: reviews çek → LLM ile özetle → product-service'e ai-report yaz → Redis cache.
On-demand endpoint ve batch scheduler aynı `summarize_product_reviews` fonksiyonunu çağırır.
"""
from __future__ import annotations

import json
import logging

from app.core.config import settings
from app.core.exceptions import AIServiceError
from app.core.llm.base import LLMMessage
from app.core.llm.factory import get_llm_client
from app.core.llm.gemini_client import safe_json_loads
from app.core.redis_client import get_redis
from app.schemas.review import ReviewSummaryResponse
from app.services.http_clients.product_client import product_client

logger = logging.getLogger(__name__)

_CACHE_PREFIX = "ai:review-summary:"

_SYSTEM_PROMPT = (
    "Sen bir e-ticaret platformunda ürün yorumlarını analiz eden bir asistansın. "
    "Sana bir ürünün müşteri yorumları verilecek. Bu yorumları Türkçe analiz et ve "
    "SADECE aşağıdaki JSON formatında yanıt ver, başka hiçbir metin ekleme:\n"
    "{\n"
    '  "summary": "2-3 cümlelik genel özet (Türkçe)",\n'
    '  "overallSentiment": "POSITIVE | NEGATIVE | MIXED",\n'
    '  "pros": ["öne çıkan olumlu nokta", "..."],\n'
    '  "cons": ["öne çıkan olumsuz nokta", "..."],\n'
    '  "keywords": ["anahtar kelime", "..."]\n'
    "}\n"
    "pros/cons en fazla 4 madde, keywords en fazla 5 kelime olsun. "
    "Yorum azsa bile elindekiyle dürüst bir özet çıkar."
)


def _build_review_text(reviews: list[dict]) -> str:
    lines: list[str] = []
    for r in reviews:
        rating = r.get("rating")
        title = (r.get("title") or "").strip()
        text = (r.get("reviewText") or "").strip()
        prefix = f"[{rating}/5]" if rating is not None else "[-]"
        body = f"{title} — {text}".strip(" —")
        if body:
            lines.append(f"{prefix} {body}")
    return "\n".join(lines)


def _average_rating(reviews: list[dict]) -> float | None:
    ratings = [r["rating"] for r in reviews if r.get("rating") is not None]
    if not ratings:
        return None
    return round(sum(ratings) / len(ratings), 2)


async def _get_cached(product_id: int) -> ReviewSummaryResponse | None:
    redis = get_redis()
    raw = await redis.get(f"{_CACHE_PREFIX}{product_id}")
    if not raw:
        return None
    try:
        data = json.loads(raw)
        data["cached"] = True
        return ReviewSummaryResponse.model_validate(data)
    except (json.JSONDecodeError, ValueError):
        return None


async def _set_cached(summary: ReviewSummaryResponse) -> None:
    redis = get_redis()
    payload = summary.model_dump(by_alias=False)
    payload["cached"] = False
    await redis.set(
        f"{_CACHE_PREFIX}{summary.product_id}",
        json.dumps(payload, ensure_ascii=False),
        ex=settings.review_summary_cache_ttl,
    )


async def summarize_product_reviews(
    product_id: int,
    *,
    use_cache: bool = True,
    persist_to_product: bool = True,
) -> ReviewSummaryResponse:
    """Bir ürünün yorumlarını özetler. Cache → LLM → product-service callback."""
    if use_cache:
        cached = await _get_cached(product_id)
        if cached:
            return cached

    reviews = await product_client.get_all_reviews(product_id)
    if not reviews:
        return ReviewSummaryResponse(
            product_id=product_id,
            review_count=0,
            average_rating=None,
            overall_sentiment=None,
            summary=None,
        )

    review_text = _build_review_text(reviews)
    avg = _average_rating(reviews)

    llm = get_llm_client()
    messages = [
        LLMMessage(role="system", content=_SYSTEM_PROMPT),
        LLMMessage(
            role="user",
            content=(
                f"Ürün için {len(reviews)} yorum (ortalama puan: {avg}):\n\n{review_text}"
            ),
        ),
    ]
    response = await llm.complete(messages, temperature=0.3, json_mode=True)
    parsed = safe_json_loads(response.content)

    summary = ReviewSummaryResponse(
        product_id=product_id,
        review_count=len(reviews),
        average_rating=avg,
        overall_sentiment=(parsed.get("overallSentiment") or "MIXED").upper(),
        summary=parsed.get("summary"),
        pros=parsed.get("pros") or [],
        cons=parsed.get("cons") or [],
        keywords=parsed.get("keywords") or [],
        cached=False,
    )

    if persist_to_product and summary.summary:
        try:
            await product_client.update_ai_report(
                product_id, _render_report(summary)
            )
        except AIServiceError as exc:
            # callback başarısız olsa da kullanıcıya özet dönmeli (best-effort)
            logger.warning("ai-report callback başarısız (product %s): %s", product_id, exc)

    await _set_cached(summary)
    return summary


def _render_report(summary: ReviewSummaryResponse) -> str:
    """product.aiReviewReport TEXT alanına yazılacak insan-okunur özet."""
    parts: list[str] = []
    if summary.summary:
        parts.append(summary.summary)
    if summary.pros:
        parts.append("Artılar: " + ", ".join(summary.pros))
    if summary.cons:
        parts.append("Eksiler: " + ", ".join(summary.cons))
    return "\n".join(parts)
