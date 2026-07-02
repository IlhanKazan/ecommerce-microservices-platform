"""Keycloak JWT doğrulama — Spring servislerindeki GlobalSecurityConfig ile aynı model.

JWKS Keycloak'tan çekilip cache'lenir; token imza + issuer + exp doğrulanır.
Audience doğrulaması yapılmaz (Spring resource-server'ı da issuer tabanlı doğruluyor).
"""
from __future__ import annotations

import time
from dataclasses import dataclass, field
from uuid import UUID

import httpx
from fastapi import Depends, Header, HTTPException, status
from jose import jwt
from jose.exceptions import JWTError

from app.core.config import settings


@dataclass
class AuthUser:
    keycloak_id: UUID
    username: str | None
    email: str | None
    first_name: str | None
    last_name: str | None
    roles: list[str] = field(default_factory=list)
    raw_token: str = ""  # downstream Spring çağrılarında forward etmek için

    def has_role(self, role: str) -> bool:
        return role in self.roles

    @property
    def display_name(self) -> str:
        if self.first_name:
            return f"{self.first_name} {self.last_name or ''}".strip()
        return self.username or "Kullanıcı"


class _JwksCache:
    """JWKS anahtarlarını bellekte tutar; kid bulunamazsa yeniler."""

    def __init__(self, ttl_seconds: int = 3600) -> None:
        self._ttl = ttl_seconds
        self._keys: dict | None = None
        self._fetched_at: float = 0.0

    def _expired(self) -> bool:
        return self._keys is None or (time.time() - self._fetched_at) > self._ttl

    async def get(self, force: bool = False) -> dict:
        if force or self._expired():
            async with httpx.AsyncClient(timeout=5.0) as client:
                resp = await client.get(settings.jwks_uri)
                resp.raise_for_status()
                self._keys = resp.json()
                self._fetched_at = time.time()
        return self._keys


_jwks_cache = _JwksCache()


async def _decode(token: str) -> dict:
    """Token'ı doğrula ve claim'leri dön. Geçersizse JWTError fırlatır."""
    options = {"verify_aud": False}
    try:
        unverified_header = jwt.get_unverified_header(token)
    except JWTError as exc:
        raise JWTError(f"Geçersiz token header: {exc}") from exc

    kid = unverified_header.get("kid")
    jwks = await _jwks_cache.get()

    # kid cache'te yoksa JWKS'i bir kez zorla yenile (key rotation senaryosu)
    if kid and not any(k.get("kid") == kid for k in jwks.get("keys", [])):
        jwks = await _jwks_cache.get(force=True)

    return jwt.decode(
        token,
        jwks,
        algorithms=["RS256"],
        issuer=settings.issuer_uri,
        options=options,
    )


def _claims_to_user(claims: dict, token: str) -> AuthUser:
    roles: list[str] = []
    resource_access = claims.get("resource_access") or {}
    client_access = resource_access.get(settings.keycloak_client_id) or {}
    roles.extend(client_access.get("roles") or [])

    return AuthUser(
        keycloak_id=UUID(claims["sub"]),
        username=claims.get("preferred_username"),
        email=claims.get("email"),
        first_name=claims.get("given_name"),
        last_name=claims.get("family_name"),
        roles=roles,
        raw_token=token,
    )


def _extract_bearer(authorization: str | None) -> str | None:
    if not authorization:
        return None
    parts = authorization.split(" ", 1)
    if len(parts) != 2 or parts[0].lower() != "bearer":
        return None
    return parts[1].strip()


async def get_current_user(
    authorization: str | None = Header(default=None),
) -> AuthUser:
    """Zorunlu auth — geçersiz/eksik token → 401."""
    token = _extract_bearer(authorization)
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authorization header gerekli",
            headers={"WWW-Authenticate": "Bearer"},
        )
    try:
        claims = await _decode(token)
        return _claims_to_user(claims, token)
    except (JWTError, KeyError, ValueError) as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Geçersiz token: {exc}",
            headers={"WWW-Authenticate": "Bearer"},
        ) from exc


async def get_optional_user(
    authorization: str | None = Header(default=None),
) -> AuthUser | None:
    """Opsiyonel auth — token yoksa None, geçersizse yine None (anonim akış)."""
    token = _extract_bearer(authorization)
    if not token:
        return None
    try:
        claims = await _decode(token)
        return _claims_to_user(claims, token)
    except (JWTError, KeyError, ValueError):
        return None


CurrentUser = Depends(get_current_user)
OptionalUser = Depends(get_optional_user)
