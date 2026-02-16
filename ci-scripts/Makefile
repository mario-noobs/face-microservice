.PHONY: help lint setup deploy-dev deploy-staging deploy-prod check test pre-commit

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

setup: ## Install dependencies (ansible, pre-commit)
	./baseline/scripts/bootstrap.sh

lint: ## Run ansible-lint on all playbooks
	cd ansible && ansible-lint .

check: ## Dry-run deploy to dev
	cd ansible && ansible-playbook --check --diff -i inventories/dev/dev.yml ../baseline/ansible/playbooks/deploy/deploy-all.yml

test: ## Run connectivity test
	cd ansible && ansible-playbook -i inventories/dev/dev.yml ../baseline/ansible/playbooks/test.yml

deploy-dev: ## Deploy all services to dev
	./scripts/deploy-dev.sh

deploy-staging: ## Deploy all services to staging
	./scripts/deploy-staging.sh

deploy-prod: ## Deploy all services to production
	./scripts/deploy-prod.sh

pre-commit: ## Run pre-commit on all files
	pre-commit run --all-files
