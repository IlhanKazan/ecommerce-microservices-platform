# syntax=docker/dockerfile:1
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY api-gateway/src ./api-gateway/src
RUN --mount=type=cache,target=/root/.m2 \
    cd api-gateway && mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder --chown=spring:spring /app/api-gateway/target/*.jar app.jar
USER spring

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
