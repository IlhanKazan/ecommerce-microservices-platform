# AI Service (backend-ai)

IlhanKazan E-Commerce platformunun FastAPI tabanlı AI katmanı.

## Özellikler

| Endpoint | Auth | Açıklama |
|---|---|---|
| `GET /api/v1/public/ai/reviews/{productId}/summary` | Public | Ürün yorumlarının AI özeti (cache + product-service callback) |
| `POST /api/v1/public/ai/track/view` | Opsiyonel | Ürün görüntüleme takibi (öneri/geçmiş) |
| `POST /api/v1/ai/products/suggest-tags` | JWT | Ürün etiketi önerisi (merchant) |
| `GET /api/v1/ai/merchants/{tenantId}/stock-insights` | JWT (üye) | Satış+stok bazlı stok önerisi |
| `POST /api/v1/ai/chat` | JWT | Alışveriş asistanı chatbot (tool calling) |

## Mimari

- **FastAPI** + async SQLAlchemy (asyncpg) + Redis + httpx
- **LLM:** OpenAI (primary) / Gemini adapter — `LLM_PROVIDER` ile seçilir
- **Auth:** Keycloak JWKS ile bağımsız JWT doğrulama (Spring servisleriyle aynı model)
- **Downstream:** product / order / stock / search servislerine HTTP; authz-hassas
  çağrılarda kullanıcı JWT'si forward edilir, internal callback'lerde service-account token
- **Batch:** APScheduler ile periyodik yorum özetleme (view edilen ürünler)

## Kurulum (dev)

```bash
cd backend-ai
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

# 1. ai_db'yi oluştur (postgres çalışıyor olmalı)
docker exec -it postgres psql -U postgres -c "CREATE DATABASE ai_db;"

# 2. Root .env'e AI değişkenlerini ekle (bkz. .env.example)

# 3. Şemayı uygula
alembic upgrade head

# 4. Servisi başlat
uvicorn app.main:app --reload --port 8091
```

Swagger: http://localhost:8091/docs

## Doğrulama

```bash
curl http://localhost:8091/health
curl http://localhost:8091/api/v1/public/ai/reviews/1/summary
curl -X POST http://localhost:8091/api/v1/ai/products/suggest-tags \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"title":"Koşu Ayakkabısı","description":"Nefes alan mesh","category":"Ayakkabı"}'
```
