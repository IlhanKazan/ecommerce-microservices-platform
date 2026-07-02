"""Ortak exception tipleri — Spring'deki BusinessException/ExternalServiceException muadili."""
from __future__ import annotations

from fastapi import HTTPException, status


class AIServiceError(Exception):
    """AI servisi temel hatası."""

    def __init__(self, message: str, code: str = "AI_ERROR") -> None:
        self.message = message
        self.code = code
        super().__init__(message)


class ExternalServiceError(AIServiceError):
    """Downstream Spring servisi çağrısı başarısız."""

    def __init__(self, message: str, code: str = "EXTERNAL_SERVICE_ERROR") -> None:
        super().__init__(message, code)


class LLMError(AIServiceError):
    """LLM sağlayıcı çağrısı başarısız."""

    def __init__(self, message: str, code: str = "LLM_ERROR") -> None:
        super().__init__(message, code)


def upstream_http_exception(exc: ExternalServiceError) -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_502_BAD_GATEWAY,
        detail={"code": exc.code, "message": exc.message},
    )


def llm_http_exception(exc: LLMError) -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail={"code": exc.code, "message": exc.message},
    )
