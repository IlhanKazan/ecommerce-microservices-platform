"""LLM client factory — LLM_PROVIDER env'ine göre sağlayıcı seçer."""
from __future__ import annotations

from functools import lru_cache

from app.core.config import settings
from app.core.llm.base import LLMClient
from app.core.llm.gemini_client import GeminiLLMClient
from app.core.llm.openai_client import OpenAILLMClient


@lru_cache
def get_llm_client() -> LLMClient:
    provider = settings.llm_provider.lower().strip()
    if provider == "gemini":
        return GeminiLLMClient()
    return OpenAILLMClient()
