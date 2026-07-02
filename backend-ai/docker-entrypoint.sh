#!/bin/sh
set -e

# Postgres hazır olana kadar bekle (depends_on healthy zaten var; ekstra güvenlik)
echo "[ai-service] Alembic migration uygulanıyor..."
alembic upgrade head

echo "[ai-service] Uvicorn başlatılıyor (port 8091)..."
exec uvicorn app.main:app --host 0.0.0.0 --port 8091 --proxy-headers
