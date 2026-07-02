"""Gemini sağlayıcı (alternatif) — google-generativeai adapter'ı.

Kanonik (OpenAI-stili) mesaj/tool formatını Gemini SDK'sına çevirir.
OpenAI primary; bu adapter LLM_PROVIDER=gemini ile devreye girer.
"""
from __future__ import annotations

import json
import logging
import uuid
from typing import Any

import google.generativeai as genai

from app.core.config import settings
from app.core.exceptions import LLMError
from app.core.llm.base import (
    LLMClient,
    LLMMessage,
    LLMResponse,
    ToolCall,
    ToolSpec,
)

logger = logging.getLogger(__name__)


class GeminiLLMClient(LLMClient):
    def __init__(self) -> None:
        if not settings.gemini_api_key:
            logger.warning("GEMINI_API_KEY boş — Gemini çağrıları başarısız olacak.")
        genai.configure(api_key=settings.gemini_api_key)
        self._model_name = settings.gemini_model

    # --- format dönüşümleri ---

    @staticmethod
    def _split_system(messages: list[LLMMessage]) -> tuple[str | None, list[LLMMessage]]:
        system_parts = [m.content for m in messages if m.role == "system" and m.content]
        rest = [m for m in messages if m.role != "system"]
        system_instruction = "\n\n".join(system_parts) if system_parts else None
        return system_instruction, rest

    @staticmethod
    def _to_gemini_contents(messages: list[LLMMessage]) -> list[dict[str, Any]]:
        contents: list[dict[str, Any]] = []
        for msg in messages:
            if msg.role == "assistant" and msg.tool_calls:
                parts = [
                    {
                        "function_call": {
                            "name": tc.name,
                            "args": tc.arguments,
                        }
                    }
                    for tc in msg.tool_calls
                ]
                contents.append({"role": "model", "parts": parts})
            elif msg.role == "tool":
                contents.append(
                    {
                        "role": "user",
                        "parts": [
                            {
                                "function_response": {
                                    "name": msg.name or "tool",
                                    "response": {"result": msg.content or ""},
                                }
                            }
                        ],
                    }
                )
            else:
                role = "model" if msg.role == "assistant" else "user"
                contents.append({"role": role, "parts": [{"text": msg.content or ""}]})
        return contents

    @staticmethod
    def _to_gemini_tools(tools: list[ToolSpec]) -> list[dict[str, Any]]:
        return [
            {
                "function_declarations": [
                    {
                        "name": t.name,
                        "description": t.description,
                        "parameters": t.parameters,
                    }
                    for t in tools
                ]
            }
        ]

    @staticmethod
    def _parse_response(response: Any) -> LLMResponse:
        text_chunks: list[str] = []
        tool_calls: list[ToolCall] = []
        try:
            candidate = response.candidates[0]
            for part in candidate.content.parts:
                fn = getattr(part, "function_call", None)
                if fn and getattr(fn, "name", None):
                    args = dict(fn.args) if fn.args else {}
                    tool_calls.append(
                        ToolCall(id=str(uuid.uuid4()), name=fn.name, arguments=args)
                    )
                elif getattr(part, "text", None):
                    text_chunks.append(part.text)
        except (AttributeError, IndexError):
            pass
        content = "".join(text_chunks) if text_chunks else None
        return LLMResponse(content=content, tool_calls=tool_calls)

    def _build_model(
        self,
        system_instruction: str | None,
        tools: list[dict[str, Any]] | None = None,
    ) -> genai.GenerativeModel:
        return genai.GenerativeModel(
            self._model_name,
            system_instruction=system_instruction,
            tools=tools,
        )

    # --- public API ---

    async def complete(
        self,
        messages: list[LLMMessage],
        *,
        temperature: float = 0.4,
        max_tokens: int | None = None,
        json_mode: bool = False,
    ) -> LLMResponse:
        system_instruction, rest = self._split_system(messages)
        gen_config: dict[str, Any] = {"temperature": temperature}
        if max_tokens:
            gen_config["max_output_tokens"] = max_tokens
        if json_mode:
            gen_config["response_mime_type"] = "application/json"
        model = self._build_model(system_instruction)
        try:
            resp = await model.generate_content_async(
                self._to_gemini_contents(rest),
                generation_config=gen_config,
            )
        except Exception as exc:  # google SDK çeşitli exception tipleri fırlatır
            raise LLMError(f"Gemini çağrısı başarısız: {exc}") from exc
        return self._parse_response(resp)

    async def complete_with_tools(
        self,
        messages: list[LLMMessage],
        tools: list[ToolSpec],
        *,
        temperature: float = 0.4,
        max_tokens: int | None = None,
    ) -> LLMResponse:
        system_instruction, rest = self._split_system(messages)
        gen_config: dict[str, Any] = {"temperature": temperature}
        if max_tokens:
            gen_config["max_output_tokens"] = max_tokens
        model = self._build_model(system_instruction, self._to_gemini_tools(tools))
        try:
            resp = await model.generate_content_async(
                self._to_gemini_contents(rest),
                generation_config=gen_config,
            )
        except Exception as exc:
            raise LLMError(f"Gemini tool çağrısı başarısız: {exc}") from exc
        return self._parse_response(resp)


# JSON parse yardımcısı (provider-agnostik) — servis katmanı kullanır
def safe_json_loads(text: str | None) -> dict[str, Any]:
    if not text:
        return {}
    cleaned = text.strip()
    # bazı modeller ```json ... ``` ile sarar
    if cleaned.startswith("```"):
        cleaned = cleaned.split("```", 2)[1]
        if cleaned.startswith("json"):
            cleaned = cleaned[4:]
    try:
        return json.loads(cleaned.strip())
    except json.JSONDecodeError:
        return {}
