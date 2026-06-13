# Gitleaks Secret Scan Raporu

**Tarih:** 2026-06-14
**Araç:** Gitleaks (latest, docker image `zricethezav/gitleaks`)
**Kapsam:** Tüm git geçmişi — 141 commit, ~3.38 MB
**İlgili roadmap maddesi:** `QUALITY-ROADMAP.md` §4 (Güvenlik / pentest → Gitleaks)

## Komut

```bash
docker run --rm -v "$(pwd):/repo" zricethezav/gitleaks:latest detect \
  --source=/repo --config=/repo/.gitleaks.toml --redact --no-banner
```

## Sonuç

| Aşama | Bulgu | Gerçek secret |
|---|---|---|
| İlk tarama (config'siz) | 2 | 0 |
| Triage sonrası (`.gitleaks.toml` allowlist) | 0 | 0 |

**Repoda sızmış gerçek secret YOK.** `.env`, Keycloak realm-export ve SPL jar manuel yönetiliyor ve commit'lenmiyor — bu da taramayla doğrulanmış oldu.

## Triage edilen bulgular (ikisi de false-positive)

| # | Dosya | Kural | Neden false-positive |
|---|---|---|---|
| 1 | `CLAUDE.md:17` | generic-api-key | Virgüllü altyapı listesi ("PostgreSQL, Kafka, Debezium, Elasticsearch, Redis, MinIO, Keycloak…") — entropy heuristic'i secret sandı. |
| 2 | `frontend/.../vite.config.ts:10` | generic-api-key | Keycloakify `themeName`/build config bloğu — secret değil. |

İki false-positive `.gitleaks.toml` içinde path-bazlı allowlist'e alındı; sonraki taramalar temiz (`no leaks found`, exit 0).

## Tekrar üretilebilirlik

- Config: repo kökünde `.gitleaks.toml` (default kural seti + triage allowlist).
- CI'a eklenebilir: `gitleaks detect --config=.gitleaks.toml` exit kodu 0 → gate yeşil.
