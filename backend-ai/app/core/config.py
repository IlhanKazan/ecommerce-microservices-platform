"""Uygulama ayarları — ana proje .env'inden okunur (backend-ai'da ayrı .env yok)."""
from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

# backend-ai/app/core/config.py -> backend-ai -> e_commerce_project
_PROJECT_ROOT = Path(__file__).resolve().parents[3]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=str(_PROJECT_ROOT / ".env"),
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    # --- Servis kimliği ---
    service_name: str = "ai-service"
    ai_service_port: int = 8091

    # --- Keycloak / Auth ---
    realm_name: str = "E-Commerce"
    # Tarayıcı/issuer public URL (token iss claim'i bununla eşleşir)
    auth_server_url: str = "http://localhost:8080"
    # Container içinden Keycloak'a erişim (JWKS + token fetch). Boşsa auth_server_url
    # kullanılır. Compose'da http://keycloak:8080 verilir (Spring jwk-set-uri muadili).
    keycloak_internal_url: str = ""
    # Rollerin map'lendiği client (resource_access.<client>.roles)
    keycloak_client_id: str = "e-commerce-backend"
    # Service account (client_credentials) — internal callback'ler için
    my_spi_client_id: str = "e-commerce-backend"
    my_spi_client_secret: str = ""

    cors_origins: str = "http://localhost:5173,http://localhost:3000"

    # --- Veritabanı (ai_db) ---
    ai_db_host: str = "localhost"
    ai_db_port: int = 5432
    ai_db_name: str = "ai_db"
    ai_db_username: str = "postgres"
    ai_db_password: str = "1234"

    # --- Redis (Spring servisleriyle ortak instance) ---
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_password: str = ""

    # --- LLM ---
    llm_provider: str = "openai"  # openai | gemini
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.0-flash"  # 1.5-flash kullanımdan kalktı

    # --- Downstream Spring servisleri ---
    product_service_url: str = "http://localhost:8084"
    order_service_url: str = "http://localhost:8088"
    stock_service_url: str = "http://localhost:8087"
    search_service_url: str = "http://localhost:8085"
    basket_service_url: str = "http://localhost:8086"
    user_tenant_service_url: str = "http://localhost:8081"

    # --- Davranış ---
    review_summary_cache_ttl: int = 3600  # saniye
    chat_session_ttl: int = 1800  # 30 dk
    chat_history_max_messages: int = 10
    review_summary_job_interval_hours: int = 2

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+asyncpg://{self.ai_db_username}:{self.ai_db_password}"
            f"@{self.ai_db_host}:{self.ai_db_port}/{self.ai_db_name}"
        )

    @property
    def redis_url(self) -> str:
        auth = f":{self.redis_password}@" if self.redis_password else ""
        return f"redis://{auth}{self.redis_host}:{self.redis_port}/0"

    @property
    def _kc_internal_base(self) -> str:
        """JWKS + token fetch için Keycloak'a erişim adresi (container DNS olabilir)."""
        return (self.keycloak_internal_url or self.auth_server_url).rstrip("/")

    @property
    def issuer_uri(self) -> str:
        """Token 'iss' claim'i bununla doğrulanır — DAİMA public URL."""
        return f"{self.auth_server_url}/realms/{self.realm_name}"

    @property
    def jwks_uri(self) -> str:
        return f"{self._kc_internal_base}/realms/{self.realm_name}/protocol/openid-connect/certs"

    @property
    def token_uri(self) -> str:
        return f"{self._kc_internal_base}/realms/{self.realm_name}/protocol/openid-connect/token"

    @property
    def cors_origin_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
