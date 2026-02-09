FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY backend/keycloak-spi /app

RUN mvn clean package -DskipTests

FROM quay.io/keycloak/keycloak:26.4.5

COPY --from=builder /app/target/*.jar /opt/keycloak/providers/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
