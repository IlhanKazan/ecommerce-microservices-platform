# syntax=docker/dockerfile:1
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY common-lib/pom.xml ./common-lib/pom.xml
COPY common-lib/src ./common-lib/src
RUN --mount=type=cache,target=/root/.m2 \
    cd common-lib && mvn install -DskipTests -q

COPY event-contracts/pom.xml ./event-contracts/pom.xml
COPY event-contracts/src ./event-contracts/src
RUN --mount=type=cache,target=/root/.m2 \
    cd event-contracts && mvn install -DskipTests -q

COPY search-service/pom.xml ./search-service/pom.xml
COPY search-service/src ./search-service/src
RUN --mount=type=cache,target=/root/.m2 \
    cd search-service && mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder --chown=spring:spring /app/search-service/target/*.jar app.jar
USER spring

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
