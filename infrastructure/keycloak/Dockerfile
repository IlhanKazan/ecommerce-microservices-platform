# ── 1) Custom SPI (kaynaktan derlenir) ───────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS spi-builder

WORKDIR /app
COPY backend/keycloak-spi /app
RUN mvn clean package -DskipTests

# ── 2) Keycloakify login/account teması (kaynaktan derlenir) ──────────────────
# Tema jar'ı burada üretilir → ne manuel `build-keycloak`, ne de commit'li jar gerekir.
# Tek `docker compose up --build` ile temiz bir makinede her şey otomatik.
#
# NOT: spi-builder ile AYNI maven 3.9 image'ından türüyoruz; Debian'ın maven 3.8.7'si
# keycloakify'ın jar paketleme adımında resolver bug'ı (".part: No such file or directory")
# veriyordu. Maven zaten image'da; üzerine sadece Node 22 kuruyoruz.
FROM maven:3.9-eclipse-temurin-21 AS theme-builder

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /theme
# Önce sadece manifest'ler → npm ci katmanı bağımlılık değişmedikçe cache'lenir
COPY frontend/e_commerce_frontend/package.json frontend/e_commerce_frontend/package-lock.json ./
RUN npm ci
# Sonra kaynak → sadece frontend değişince yeniden derlenir
COPY frontend/e_commerce_frontend/ ./
RUN npm run build-keycloak
# Çıktı: /theme/dist_keycloak/keycloak-theme-for-kc-*.jar

# ── 3) Final Keycloak image ───────────────────────────────────────────────────
FROM quay.io/keycloak/keycloak:26.4.5

COPY --from=spi-builder   /app/target/*.jar /opt/keycloak/providers/
# vite.config keycloakVersionTargets → sadece KC 26.2+ jar'ı, sabit isimle üretilir.
COPY --from=theme-builder /theme/dist_keycloak/ilhan-e-ticaret.jar /opt/keycloak/providers/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
