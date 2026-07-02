"""Auto-tag şemaları."""
from __future__ import annotations

from pydantic import BaseModel, Field


class TagSuggestRequest(BaseModel):
    title: str = Field(..., min_length=2, max_length=300)
    description: str | None = Field(None, max_length=5000)
    category: str | None = Field(None, max_length=200)


class TagSuggestResponse(BaseModel):
    tags: list[str] = Field(default_factory=list)
