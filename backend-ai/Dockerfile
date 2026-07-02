# AI Service (FastAPI) — multi-stage değil; tek aşama yeterli (Python).
FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# Önce bağımlılıklar (layer cache)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Uygulama + migration dosyaları
COPY app ./app
COPY alembic ./alembic
COPY alembic.ini .
COPY docker-entrypoint.sh .

# Non-root kullanıcı (Java servisleriyle aynı güvenlik pratiği)
RUN adduser --disabled-password --gecos "" appuser \
    && chmod +x docker-entrypoint.sh \
    && chown -R appuser:appuser /app
USER appuser

EXPOSE 8091

# Başlangıçta migration uygula, sonra servisi başlat
ENTRYPOINT ["./docker-entrypoint.sh"]
