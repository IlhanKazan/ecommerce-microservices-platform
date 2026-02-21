#!/bin/bash

# Hata olursa dur
set -e

echo "Platform durduruluyor..."
sudo docker compose down --remove-orphans

echo "Eski Kafka ve Zookeeper verileri temizleniyor..."
sudo rm -rf ./infrastructure/kafka_data
sudo rm -rf ./infrastructure/zookeeper_data

echo "Yeni klasörler oluşturuluyor..."
mkdir -p ./infrastructure/kafka_data
mkdir -p ./infrastructure/zookeeper_data

echo "İzinler ayarlanıyor (User ID 1000 - Kafka için kritik)..."
sudo chown -R 1000:1000 ./infrastructure/kafka_data
sudo chown -R 1000:1000 ./infrastructure/zookeeper_data

echo "Platform ayağa kaldırılıyor..."
sudo docker compose up -d

echo "Platform temiz kurulumla açıldı!"
echo "Debezium ve Kafka'nın kendine gelmesi 30-60 sn sürebilir."
echo "Logları izlemek için: sudo docker compose logs -f"
echo "Logları tek tek görmek için: sudo docker logs <container_adi>"
echo "Genel bakis icin: sudo docker compose ps"
