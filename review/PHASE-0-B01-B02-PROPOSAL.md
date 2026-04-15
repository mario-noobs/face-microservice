# Phase 0 -- B-01 & B-02 Implementation Proposal

> **SUPERSEDED**: The monorepo-shaped design below was replaced by the orchestrator-manifest model in [ADR-005](../adr/ADR-005-orchestrator-versions-manifest.md). Services build and publish images from their own repos (B-01 was already done there); this repo receives `repository_dispatch` events, updates `versions.yaml`, and deploys. Rollback (B-02) uses `git revert` on the manifest plus `make deploy-previous`. Kept for historical trace.

**Goal**: Eliminate manual `image_tag` entry from the deploy workflow and provide a safe, fast rollback path.
**Backlog**: B-01 (auto build-and-push), B-02 (rollback path)
**Roadmap**: [Phase 0](ROADMAP.md#phase-0--stabilize-delivery-p0)
**AUDIT**: [D-1, D-3](AUDIT.md)

## Current State

- `deploy.yml` is the only deploy workflow. It is `workflow_dispatch`-only and requires a human to paste an `image_tag` string. This is the D-1 pain.
- No workflow builds or pushes images on merge to `main`. The `e2e.yml` workflow builds locally for test but never pushes to GHCR.
- Baseline provides `_docker-build.yml` (reusable, outputs `image_tag`) and `_deploy.yml` (reusable, accepts `image_tag`). Both are ready to call.
- Baseline `deploy-service.yml` backs up `.env` to `.env.bak` before writing a new tag. One-step backup only.
- Baseline `rollback.yml` restores `.env.bak` and restarts the service. It cannot roll back more than one step. No tag history file exists on the VM.
- `ci-scripts/Makefile` has `deploy-dev`, `deploy-staging`, `deploy-prod` targets. No `deploy-previous` target exists.
- Baseline `deploy.sh` wrapper is hardcoded to call `deploy-all.yml`. It does not accept a playbook argument.
- Service directory names diverge from deploy target names: `face-ai-service/` directory deploys as `face-recognition-service`.

## Proposed Changes

### B-01: Auto build-and-push on merge to main

**New files** (one workflow per service):
- `.github/workflows/build-backend-service.yml`: triggers on push to `main` with path filter `backend-service/**`
- `.github/workflows/build-face-recognition-service.yml`: triggers on push to `main` with path filter `face-ai-service/**`
- `.github/workflows/build-gui-app.yml`: triggers on push to `main` with path filter `gui-app/**`

**Modified files**:
- `.github/workflows/deploy.yml`: keep as-is for manual override / staging / production deploys. Make `image_tag` optional (default to empty string) so the baseline `_deploy.yml` can fall back to `latest` when not provided. This preserves the escape hatch.

**Workflow shape** (per service, e.g., `build-backend-service.yml`):

```
name: Build backend-service
on:
  push:
    branches: [main]
    paths: [backend-service/**]

jobs:
  build:
    uses: mario-noobs/ci-baseline/.github/workflows/_docker-build.yml@main
    with:
      image_name: backend-service
      dockerfile: backend-service/Dockerfile
      context: backend-service
    permissions:
      contents: read
      packages: write

  deploy-dev:                          # conditional on build success
    needs: build
    if: <gated by open question Q1>
    uses: mario-noobs/ci-baseline/.github/workflows/_deploy.yml@main
    with:
      environment: dev
      service: backend-service
      image_tag: ${{ needs.build.outputs.image_tag }}
      ci_scripts_path: ci-scripts
    secrets:
      SSH_PRIVATE_KEY: ${{ secrets.SSH_PRIVATE_KEY }}
      ANSIBLE_VAULT_PASSWORD: ${{ secrets.ANSIBLE_VAULT_PASSWORD }}
```

Each service workflow follows this pattern. The `image_name`, `dockerfile`, `context`, `service`, and `paths` differ per service:

| Service dir | `image_name` | `service` (deploy target) | `paths` filter |
|---|---|---|---|
| `backend-service/` | `backend-service` | `backend-service` | `backend-service/**` |
| `face-ai-service/` | `face-recognition-service` | `face-recognition-service` | `face-ai-service/**` |
| `gui-app/` | `gui-app` | `gui-app` | `gui-app/**` |

**Tag strategy**: baseline `_docker-build.yml` already tags with `sha-<short>`, branch name, and `latest` on default branch. No changes needed.

**`make deploy-dev` alignment**: once auto-deploy-to-dev is enabled (see Q1), `make deploy-dev` becomes a "redeploy current state" command. It calls `deploy-all.yml`, which reads tags from the VM's `.env` file. This already works -- the auto-deploy job will have written the new tag into `.env` via `deploy-service.yml`. No Makefile changes needed for B-01.

### B-02: Rollback path

**Tag retention strategy**: two layers.

1. **GHCR**: images tagged `sha-<hash>` are retained indefinitely by default (GHCR has no auto-expiry). The existing `cleanup-images.yml` baseline playbook prunes the VM host, not GHCR. No GHCR retention policy change needed for Phase 0.
2. **VM-side tag history**: introduce a `.env.history` file on the VM that records the last N tag values per service, one line per deploy, format: `<timestamp> <service> <tag>`. This enables rollback beyond one step.

**Approach decision -- baseline gap**:

Baseline `rollback.yml` only supports one-step rollback via `.env.bak`. Two options:

- **(a) Ship B-02 minimal**: use baseline's existing `rollback.yml` as-is. `make deploy-previous` calls it. Limitation: one step back only. If you deploy twice without rolling back, the first tag is lost.
- **(b) Upstream N-step history**: add a `tag-history` mechanism to `ci-baseline` (append to `.env.history` on each deploy, rollback reads from it). This gives N-step rollback.

**Recommendation**: ship (a) now, track (b) as a follow-up. Rationale: solo developer, no production users, one-step covers 90% of "I just deployed a bad image" scenarios. Upstreaming to baseline is a separate PR with its own review cycle.

**New Makefile targets** in `ci-scripts/Makefile`:

- `deploy-previous`: rolls back a single service on dev. Requires `SERVICE=<name>` argument. Calls a new script `ci-scripts/scripts/rollback.sh` which runs `ansible-playbook -i inventories/<env>/<env>.yml ../baseline/ansible/playbooks/deploy/rollback.yml -e target_service=<service>`.
- `rollback-dev`, `rollback-staging`, `rollback-prod`: environment-specific variants (optional, see Q3).

**New files**:
- `ci-scripts/scripts/rollback.sh`: thin wrapper (mirrors `deploy.sh` pattern) that calls baseline `rollback.yml` with `-e target_service=$SERVICE`.

**Modified files**:
- `ci-scripts/Makefile`: add `deploy-previous` target (and env-specific variants if decided).

**New workflow** (optional, see Q4):
- `.github/workflows/rollback.yml`: `workflow_dispatch` with inputs `environment` and `service`. Calls baseline `_deploy.yml` is not suitable here (it deploys forward). Instead, it would run `ansible-playbook` directly against `rollback.yml`. Alternatively, skip this workflow and rely on `make deploy-previous` from the operator's machine for Phase 0.

## Open Questions / Decisions Needed

1. **Q1: Auto-deploy to dev on build success?** Recommend yes -- merge to `main` builds the image and immediately deploys to `dev`. Staging and production remain manual via `deploy.yml` with GitHub environment protection rules. If no, the build workflows only push images and `deploy-dev` stays manual.

2. **Q2: Keep `deploy.yml` manual `image_tag` input as escape hatch?** Recommend yes -- make the field optional with a sensible default (e.g., `latest`). This lets you override for staging/prod deploys where you pick a specific SHA. Removing it entirely leaves no manual deploy path.

3. **Q3: `make deploy-previous` scope -- per-service only, or full-stack?** Baseline `rollback.yml` operates on a single service. Rolling back the full stack would require calling it three times (or writing a `rollback-all.yml`). Recommend per-service only for Phase 0: `make deploy-previous SERVICE=backend-service ENV=dev`.

4. **Q4: Do we need a GitHub Actions workflow for rollback, or is `make deploy-previous` from the operator machine sufficient for Phase 0?** Recommend Makefile-only for now. A workflow adds value when multiple people deploy or when you want audit trail in GitHub -- not critical for a solo project yet.

5. **Q5: Accept baseline's 1-step rollback limit, or upstream N-step history now?** Recommend accept 1-step for Phase 0. Track N-step as a separate backlog item. The 1-step model covers the immediate "bad deploy" case.

6. **Q6: B-03 overlap -- baseline `deploy-service.yml` already does `docker compose pull` + `up -d --no-deps`, which is essentially what B-03 (fast redeploy) describes. Should we close B-03 as already-done, or does B-03 imply something beyond what baseline provides (e.g., systemd unit management, zero-downtime restart)?** This affects whether Phase 0 scope shrinks.

## Success Criteria (from ROADMAP Phase 0)

- [ ] Merge to `main` auto-builds and pushes an image per service (path-filtered, no false triggers).
- [ ] `make deploy-dev` pulls the latest tag without manual `image_tag` input.
- [ ] `make deploy-previous SERVICE=<name> ENV=dev` rolls back in under 2 minutes.
- [ ] Manual `deploy.yml` workflow still works for staging/production with explicit tag.

## Non-goals for this increment

- Canary or staged rollout (D-4 -- deferred to later phase).
- Multi-region deploy.
- N-step rollback history (defer to baseline upstream PR).
- GHCR image retention policy / pruning (existing `cleanup-images.yml` on host is sufficient).
- B-03 fast-redeploy evaluation (separate ticket, flagged in Q6).
- Trivy on PR (B-09, separate).
- face-ai-service CI tests (B-06, separate).
- Secrets consolidation (B-21, separate).
