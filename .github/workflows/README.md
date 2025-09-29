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

  * `master`
  * `30-setup-ci-pipeline` (temporary - would be better to be removed for the issue to be closed)
* Pull requests targeting the `master` branch.

## Running the Workflow

1. **Create or update a PR** targeting `master`.
2. GitHub Actions will automatically trigger the workflow.
3. Monitor the workflow run in the **Actions** tab of the repository.

### Debugging Failures

If any job fails, a debug step is included that outputs:

* Current directory
* Top-level files and folders
* Recursive listing of all files
* Checks for key files (`gradlew`, `requirements.txt`, `package.json`)

This helps identify repository layout issues in the runner environment.

### Key Notes

* Due to GitHub Actions checkout behavior, the repository may appear in a nested folder inside the runner workspace. The workflow explicitly uses relative paths or `--prefix` to correctly locate service directories.
* If builds fail due to missing files, check the debug logs to verify the actual file paths in the runner workspace.

## Contributing

* For any changes to the workflow, ensure paths to services are updated correctly.
* When adding new services, follow the same pattern with proper working directories and commands.
