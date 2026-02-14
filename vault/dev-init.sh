#!/bin/bash
set -e

echo "Waiting for Vault to be ready..."
until vault status 2>/dev/null; do
  sleep 1
done

echo "Enabling KV v2 secrets engine..."
vault secrets enable -path=secret -version=2 kv 2>/dev/null || true

echo "Seeding dev secrets..."
vault kv put secret/backend-service/dev \
  spring.datasource.password=root_password_secret_tcp \
  jwt.secret=dmVyeS1pbXBvcnRhbnQtcGxlYXNlLWNoYW5nZS1pdC1pbi1wcm9kdWN0aW9uLXdpdGgtYXQtbGVhc3QtMjU2LWJpdHM= \
  minio.access-key=admin \
  minio.secret-key=123456789 \
  spring.data.redis.password= \
  face-recognition.service-url=http://face-recognition-service:5000

echo "Seeding prod secrets (placeholder values)..."
vault kv put secret/backend-service/prod \
  spring.datasource.password=CHANGE_ME_PROD_DB_PASSWORD \
  jwt.secret=CHANGE_ME_PROD_JWT_SECRET_BASE64 \
  minio.access-key=CHANGE_ME_PROD_MINIO_ACCESS \
  minio.secret-key=CHANGE_ME_PROD_MINIO_SECRET \
  spring.data.redis.password=CHANGE_ME_PROD_REDIS_PASSWORD \
  face-recognition.service-url=http://face-recognition-service:5000

echo "Creating Vault policies..."
vault policy write backend-dev /vault/policies/backend-dev.hcl
vault policy write backend-prod /vault/policies/backend-prod.hcl
vault policy write ci-cd /vault/policies/ci-cd.hcl

echo "Enabling AppRole auth method..."
vault auth enable approle 2>/dev/null || true

echo "Creating dev AppRole..."
vault write auth/approle/role/backend-dev \
  token_policies="backend-dev" \
  token_ttl=1h \
  token_max_ttl=4h \
  secret_id_ttl=0

echo "Creating prod AppRole..."
vault write auth/approle/role/backend-prod \
  token_policies="backend-prod" \
  token_ttl=1h \
  token_max_ttl=4h \
  secret_id_ttl=24h \
  secret_id_num_uses=1

echo "Vault initialization complete!"
echo "Dev role-id:"
vault read auth/approle/role/backend-dev/role-id