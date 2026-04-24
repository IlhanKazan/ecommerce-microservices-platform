#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONNECTOR_DIR="$(cd "${SCRIPT_DIR}/../debezium" && pwd)"

if [ -f ../../.env ]; then
  source ../../.env
  export POSTGRES_USER POSTGRES_PASSWORD
else
  echo "UYARI: .env dosyası bulunamadı! Değişkenler boş gidebilir."
fi

if [ ! -d "${CONNECTOR_DIR}" ]; then
  echo "HATA: ${CONNECTOR_DIR} klasörü bulunamadı."
  exit 1
fi

echo "Debezium Connect servisinin hazır olması bekleniyor..."

until curl -s -f -o /dev/null "http://localhost:8083/"; do
  echo "Bekleniyor (Debezium henüz uyanmadı)..."
  sleep 5
done

echo "Debezium uyandı!"

register_connector() {
  local file="$1"
  local name="$2"

  if [ ! -f "${CONNECTOR_DIR}/${file}" ]; then
    echo "HATA: ${CONNECTOR_DIR}/${file} bulunamadı, atlıyorum."
    return 1
  fi

  echo ""
  echo "==> ${name} varsa siliniyor..."
  curl -s -X DELETE "http://localhost:8083/connectors/${name}" > /dev/null || true
  sleep 2

  echo "==> ${name} yükleniyor..."
  envsubst '${POSTGRES_USER} ${POSTGRES_PASSWORD}' < "${CONNECTOR_DIR}/${file}" \
    | curl -i -X POST \
        -H "Accept:application/json" \
        -H "Content-Type:application/json" \
        http://localhost:8083/connectors/ \
        -d @-
  echo ""
}

register_connector "user-tenant-connector.json" "user-tenant-service-connector"
register_connector "product-connector.json"     "product-service-connector"
register_connector "stock-connector.json"       "stock-service-connector"
register_connector "payment-connector.json"     "payment-service-connector"

echo ""
echo "Tüm Connector'lar yüklendi!"