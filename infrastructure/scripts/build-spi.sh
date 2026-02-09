#!/bin/bash

GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${GREEN}SPI Projesi derleniyor...${NC}"

cd "$(dirname "$0")/../../backend/keycloak-spi-user-provider" || exit

mvn clean package -DskipTests

TARGET_DIR="../../infrastructure/keycloak/providers"
mkdir -p "$TARGET_DIR"

cp target/*.jar "$TARGET_DIR/"

echo -e "${GREEN}Başarılı! JAR dosyası altyapı klasörüne kopyalandı.${NC}"
