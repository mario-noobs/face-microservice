# CI Pipeline Workflow Instructions

This document provides instructions to run the CI pipeline for the `face-microservice` repository, covering Java, Python, and Node.js services.

## Overview

The CI workflow is implemented using GitHub Actions and performs the following jobs:

1. **Java Build** (`face-reg-engine`) - builds and tests the Java backend.
2. **Python Tests** (`face-regconition-service`) - installs dependencies and runs Python unit tests.
3. **Node.js Build & Lint** (`gui-app`) - installs dependencies, runs linting, and builds the frontend.

## Pre-requisites

* GitHub account with access to the repository.
* GitHub Actions enabled for the repository.
* No local setup required; all jobs run in GitHub-hosted runners.

## Workflow Trigger

The CI workflow runs automatically on:

* Pushes to the following branches:
  * `develop`
  * `release/*` branches
* Pull requests targeting the `develop` branch.

## Running the Workflow

1. **Create or update a PR** targeting `develop`.
2. GitHub Actions will automatically trigger the workflow.
3. Monitor the workflow run in the **Actions** tab of the repository.

### Important: Git Submodules

This repository uses Git submodules for all services. The CI workflow automatically checks out submodules using:

```yaml
- name: Checkout repository
  uses: actions/checkout@v4
  with:
    submodules: recursive
```

This ensures all service code is available for building and testing.

### Debugging Failures

If any job fails, a debug step is included that outputs:

* Current directory
* Top-level files and folders
* Recursive listing of all files
* Checks for key files (`gradlew`, `requirements.txt`, `package.json`)

This helps identify repository layout issues in the runner environment.

### Key Notes

* The repository uses Git submodules for each microservice (face-reg-engine, face-regconition-service, gui-app, auth-service, profile-service, ai-backend-service).
* The workflow uses direct paths to submodule directories (e.g., `face-reg-engine/gradlew`, not `face-microservice/face-reg-engine/gradlew`).
* If builds fail due to missing files, verify that submodules are being checked out correctly in the workflow.

## Contributing

* For any changes to the workflow, ensure paths to services are updated correctly.
* When adding new services, follow the same pattern with proper working directories and commands.
