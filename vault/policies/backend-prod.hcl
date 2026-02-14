# Allow read access to prod secrets
path "secret/data/backend-service/prod" {
  capabilities = ["read"]
}

path "secret/metadata/backend-service/prod" {
  capabilities = ["read", "list"]
}

# Deny access to dev secrets
path "secret/data/backend-service/dev" {
  capabilities = ["deny"]
}