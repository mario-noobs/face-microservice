# Allow read access to dev secrets
path "secret/data/backend-service/dev" {
  capabilities = ["read"]
}

path "secret/metadata/backend-service/dev" {
  capabilities = ["read", "list"]
}

# Deny access to prod secrets
path "secret/data/backend-service/prod" {
  capabilities = ["deny"]
}