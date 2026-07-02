"""Review summary şemaları."""
from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field

Sentiment = Literal["POSITIVE", "NEGATIVE", "MIXED", "NEUTRAL"]


class ReviewSummaryResponse(BaseModel):
    product_id: int = Field(..., serialization_alias="productId")
    review_count: int = Field(..., serialization_alias="reviewCount")
    average_rating: float | None = Field(None, serialization_alias="averageRating")
    overall_sentiment: Sentiment | None = Field(
        None, serialization_alias="overallSentiment"
    )
    summary: str | None = None
    pros: list[str] = Field(default_factory=list)
    cons: list[str] = Field(default_factory=list)
    keywords: list[str] = Field(default_factory=list)
    cached: bool = False

    model_config = {"populate_by_name": True}
