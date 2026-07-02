"""LLM sağlayıcı soyutlaması — OpenAI ve Gemini arkasında ortak arayüz.

Tasarım: provider'lar OpenAI'nin mesaj/tool şemasını "kanonik" format olarak kabul eder.
Gemini adapter'ı bu kanonik formatı kendi SDK'sına çevirir. Böylece servis katmanı
tek bir arayüzle konuşur, sağlayıcı `LLM_PROVIDER` env'iyle seçilir.
"""
from __future__ import annotations

import abc
from dataclasses import dataclass, field
from typing import Any, Literal

Role = Literal["system", "user", "assistant", "tool"]


@dataclass
class LLMMessage:
    role: Role
    content: str | None = None
    # assistant'ın tetiklediği tool çağrıları (varsa)
    tool_calls: list[ToolCall] = field(default_factory=list)
    # tool sonucu mesajıysa hangi çağrıya ait
    tool_call_id: str | None = None
    name: str | None = None  # tool adı (tool mesajları için)


@dataclass
class ToolCall:
    id: str
    name: str
    arguments: dict[str, Any]


@dataclass
class ToolSpec:
    """LLM'e sunulan tool tanımı (JSON-Schema parametreleri)."""

    name: str
    description: str
    parameters: dict[str, Any]


@dataclass
class LLMResponse:
    content: str | None
    tool_calls: list[ToolCall] = field(default_factory=list)

    @property
    def wants_tools(self) -> bool:
        return len(self.tool_calls) > 0


class LLMClient(abc.ABC):
    """Tüm LLM sağlayıcılarının uyguladığı sözleşme."""

    @abc.abstractmethod
    async def complete(
        self,
        messages: list[LLMMessage],
        *,
        temperature: float = 0.4,
        max_tokens: int | None = None,
        json_mode: bool = False,
    ) -> LLMResponse:
        """Düz metin / JSON tamamlama (tool yok)."""

    @abc.abstractmethod
    async def complete_with_tools(
        self,
        messages: list[LLMMessage],
        tools: list[ToolSpec],
        *,
        temperature: float = 0.4,
        max_tokens: int | None = None,
    ) -> LLMResponse:
        """Tool calling destekli tamamlama — model tool çağırabilir."""
