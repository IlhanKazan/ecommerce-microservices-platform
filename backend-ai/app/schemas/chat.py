"""Chatbot şemaları."""
from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=2000)
    session_id: UUID | None = Field(None, serialization_alias="sessionId")

    model_config = {"populate_by_name": True}


class ChatResponse(BaseModel):
    session_id: UUID = Field(..., serialization_alias="sessionId")
    message: str
    used_tools: list[str] = Field(default_factory=list, serialization_alias="usedTools")

    model_config = {"populate_by_name": True}
