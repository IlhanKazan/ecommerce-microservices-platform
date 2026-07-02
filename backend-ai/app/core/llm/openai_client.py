"""OpenAI sağlayıcı (primary) — AsyncOpenAI tabanlı."""
from __future__ import annotations

import json
import logging
from typing import Any

from openai import AsyncOpenAI, OpenAIError

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


class OpenAILLMClient(LLMClient):
    def __init__(self) -> None:
        if not settings.openai_api_key:
            logger.warning("OPENAI_API_KEY boş — OpenAI çağrıları başarısız olacak.")
        self._client = AsyncOpenAI(api_key=settings.openai_api_key)
        self._model = settings.openai_model
        # gpt-5 / o1 / o3 / o4 reasoning modelleri: custom temperature desteklemez
        # (yalnız default=1) ve max_tokens yerine max_completion_tokens ister.
        m = self._model.lower()
        self._is_reasoning = (
            m.startswith("gpt-5") or m.startswith("o1") or m.startswith("o3") or m.startswith("o4")
        )

    def _apply_sampling(
        self, kwargs: dict[str, Any], temperature: float, max_tokens: int | None
    ) -> None:
        """Model ailesine göre temperature / token parametrelerini ayarlar."""
        if self._is_reasoning:
            # custom temperature reddedilir → hiç gönderme (default=1 kullanılır)
            if max_tokens:
                kwargs["max_completion_tokens"] = max_tokens
        else:
            kwargs["temperature"] = temperature
            if max_tokens:
                kwargs["max_tokens"] = max_tokens

    # --- format dönüşümleri ---

    @staticmethod
    def _to_openai_message(msg: LLMMessage) -> dict[str, Any]:
        if msg.role == "assistant" and msg.tool_calls:
            return {
                "role": "assistant",
                "content": msg.content or "",
                "tool_calls": [
                    {
                        "id": tc.id,
                        "type": "function",
                        "function": {
                            "name": tc.name,
                            "arguments": json.dumps(tc.arguments, ensure_ascii=False),
                        },
                    }
                    for tc in msg.tool_calls
                ],
            }
        if msg.role == "tool":
            return {
                "role": "tool",
                "tool_call_id": msg.tool_call_id,
                "content": msg.content or "",
            }
        return {"role": msg.role, "content": msg.content or ""}

    @staticmethod
    def _to_openai_tool(tool: ToolSpec) -> dict[str, Any]:
        return {
            "type": "function",
            "function": {
                "name": tool.name,
                "description": tool.description,
                "parameters": tool.parameters,
            },
        }

    @staticmethod
    def _parse_response(choice: Any) -> LLMResponse:
        message = choice.message
        tool_calls: list[ToolCall] = []
        for tc in message.tool_calls or []:
            try:
                args = json.loads(tc.function.arguments or "{}")
            except json.JSONDecodeError:
                args = {}
            tool_calls.append(
                ToolCall(id=tc.id, name=tc.function.name, arguments=args)
            )
        return LLMResponse(content=message.content, tool_calls=tool_calls)

    # --- public API ---

    async def complete(
        self,
        messages: list[LLMMessage],
        *,
        temperature: float = 0.4,
        max_tokens: int | None = None,
        json_mode: bool = False,
    ) -> LLMResponse:
        kwargs: dict[str, Any] = {
            "model": self._model,
            "messages": [self._to_openai_message(m) for m in messages],
        }
        self._apply_sampling(kwargs, temperature, max_tokens)
        if json_mode:
            kwargs["response_format"] = {"type": "json_object"}
        try:
            resp = await self._client.chat.completions.create(**kwargs)
        except OpenAIError as exc:
            raise LLMError(f"OpenAI çağrısı başarısız: {exc}") from exc
        return self._parse_response(resp.choices[0])

    async def complete_with_tools(
        self,
        messages: list[LLMMessage],
        tools: list[ToolSpec],
        *,
        temperature: float = 0.4,
        max_tokens: int | None = None,
    ) -> LLMResponse:
        kwargs: dict[str, Any] = {
            "model": self._model,
            "messages": [self._to_openai_message(m) for m in messages],
            "tools": [self._to_openai_tool(t) for t in tools],
            "tool_choice": "auto",
        }
        self._apply_sampling(kwargs, temperature, max_tokens)
        try:
            resp = await self._client.chat.completions.create(**kwargs)
        except OpenAIError as exc:
            raise LLMError(f"OpenAI tool çağrısı başarısız: {exc}") from exc
        return self._parse_response(resp.choices[0])
