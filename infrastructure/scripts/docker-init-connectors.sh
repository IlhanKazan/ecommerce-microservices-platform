#!/bin/sh
# Debezium connector'larını Docker network üzerinden kaydeder.
# register_connector.sh'ın container içi versiyonu — localhost yerine debezium-connect DNS kullanır.
# Compose'dan POSTGRES_USER ve POSTGRES_PASSWORD env var olarak gelir.
set -e

DEBEZIUM_URL="http://debezium-connect:8083"

register() {
  file="$1"
  name=$(grep '"name"' "$file" | head -1 | sed 's/.*"name"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/')

  echo "==> Deleting existing connector (if any): $name"
  curl -sf -X DELETE "$DEBEZIUM_URL/connectors/$name" || true
  sleep 1

  echo "==> Registering: $name"
  sed "s/\${POSTGRES_USER}/$POSTGRES_USER/g; s/\${POSTGRES_PASSWORD}/$POSTGRES_PASSWORD/g" "$file" \
    | curl -sf -X POST \
        -H "Accept:application/json" \
        -H "Content-Type:application/json" \
        "$DEBEZIUM_URL/connectors/" \
        -d @-
  echo ""
  echo "    OK: $name"
}

register /connectors/user-tenant-connector.json
register /connectors/product-connector.json
register /connectors/stock-connector.json
register /connectors/payment-connector.json

echo ""
echo "All connectors registered!"
