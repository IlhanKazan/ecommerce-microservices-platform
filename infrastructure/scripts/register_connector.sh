#!/bin/bash

if [ -f ../../.env ]; then
  source ../../.env
else
  echo "UYARI: .env dosyası bulunamadı! Değişkenler boş gidebilir."
fi

echo "Debezium Connect servisinin hazır olması bekleniyor..."

until curl -s -f -o /dev/null "http://localhost:8083/"; do
  echo "Bekleniyor (Debezium henüz uyanmadı)..."
  sleep 5
done

echo "Debezium uyandı! User Tenant Connector yükleniyor..."

curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8083/connectors/ -d @- <<EOF
{
  "name": "user-tenant-service-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "${POSTGRES_USER}",
    "database.password": "${POSTGRES_PASSWORD}",
    "database.dbname": "user_tenant_db",
    "topic.prefix": "user-tenant-service",
    "plugin.name": "pgoutput",
    "table.include.list": "public.outbox",
    "publication.autocreate.mode": "filtered",
    "slot.name": "user_tenant_service_debezium_slot",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.route.topic.replacement": "\${routedByValue}",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.payload": "message_payload",
    "transforms.outbox.table.field.event.timestamp": "created_at",
    "transforms.outbox.route.by.field": "aggregate_type"
  }
}
EOF

echo "Product Service Connector yükleniyor..."
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8083/connectors/ -d @- <<EOF
{
  "name": "product-service-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "${POSTGRES_USER}",
    "database.password": "${POSTGRES_PASSWORD}",
    "database.dbname": "product_catalog_db",
    "topic.prefix": "product-service",
    "plugin.name": "pgoutput",
    "table.include.list": "public.outbox",
    "publication.autocreate.mode": "filtered",
    "slot.name": "product_service_debezium_slot",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.route.topic.replacement": "\${routedByValue}",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.payload": "message_payload",
    "transforms.outbox.table.field.event.timestamp": "created_at",
    "transforms.outbox.route.by.field": "aggregate_type"
  }
}
EOF

echo "Stock Service Connector yükleniyor..."
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" http://localhost:8083/connectors/ -d @- <<EOF
{
  "name": "stock-service-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "${POSTGRES_USER}",
    "database.password": "${POSTGRES_PASSWORD}",
    "database.dbname": "stock_db",
    "topic.prefix": "stock-service",
    "plugin.name": "pgoutput",
    "table.include.list": "public.outbox",
    "publication.autocreate.mode": "filtered",
    "slot.name": "stock_service_debezium_slot",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.route.topic.replacement": "\${routedByValue}",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.payload": "message_payload",
    "transforms.outbox.table.field.event.timestamp": "created_at",
    "transforms.outbox.route.by.field": "aggregate_type"
  }
}
EOF

echo ""
echo "Tüm Connector'lar yüklendi!"
