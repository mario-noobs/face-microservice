# CI/CD policy - read both envs, manage AppRole secret-ids
path "secret/data/backend-service/*" {
  capabilities = ["read"]
}

path "secret/metadata/backend-service/*" {
  capabilities = ["read", "list"]
}

# Allow generating secret-ids for deployment
path "auth/approle/role/backend-*/secret-id" {
  capabilities = ["create", "update"]
}

path "auth/approle/role/backend-*/role-id" {
  capabilities = ["read"]
}