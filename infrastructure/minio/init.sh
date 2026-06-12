#!/bin/sh
# ============================================================
# MinIO init — bucket + public-read + scoped app service account
# minio/mc tabanlı tek-seferlik container'dan idempotent çalışır.
# Gerekli env: MINIO_ROOT_USER, MINIO_ROOT_PASSWORD (admin),
#              MINIO_ACCESS_KEY, MINIO_SECRET_KEY (app account),
#              USER_MINIO_BUCKET
# ============================================================
set -e

echo "[minio-init] MinIO bağlantısı bekleniyor..."
until mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1; do
  echo "[minio-init] MinIO henüz hazır değil, 2sn..."
  sleep 2
done
echo "[minio-init] MinIO hazır."

# 1. Bucket (idempotent)
mc mb --ignore-existing "local/$USER_MINIO_BUCKET"

# 2. Public-read: SADECE anonim GetObject (/*); ListBucket (enumerate) KAPALI.
# `mc anonymous set download` preset'i anonim s3:ListBucket de veriyordu (enumerate açık
# kalıyordu); custom policy ile sadece obje okuma açık. set-json bucket anonim policy'sini ezer.
cat > /tmp/anon-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"AWS": ["*"]},
    "Action": ["s3:GetObject"],
    "Resource": ["arn:aws:s3:::${USER_MINIO_BUCKET}/*"]
  }]
}
EOF
mc anonymous set-json /tmp/anon-policy.json "local/$USER_MINIO_BUCKET"

# 3. Bucket-scoped policy — placeholder'ı gerçek bucket adıyla değiştir
# Not: minio/mc image'ında sed/awk yok; shell substitution kullanıyoruz.
POLICY_TEMPLATE=$(cat /policies/app-policy.json)
printf '%s' "${POLICY_TEMPLATE//__BUCKET__/$USER_MINIO_BUCKET}" > /tmp/app-policy.json
mc admin policy create local ecommerce-app-policy /tmp/app-policy.json || \
  echo "[minio-init] policy zaten var, atlandı"

# 4. App service account (root DEĞİL) — app bu credential ile bağlanır
mc admin user add local "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" || \
  echo "[minio-init] app user zaten var, atlandı"
mc admin policy attach local ecommerce-app-policy --user "$MINIO_ACCESS_KEY" || \
  echo "[minio-init] policy zaten attach, atlandı"

echo "[minio-init] Tamamlandı: bucket=$USER_MINIO_BUCKET, app-user=$MINIO_ACCESS_KEY"
