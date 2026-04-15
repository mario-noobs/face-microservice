# ADR-005: Orchestrator Repo with Versions Manifest (Retire Submodules)

- **Status**: Accepted
- **Date**: 2026-04-15
- **Deciders**: Mario Bui (solo)
- **Related**: [ADR-001](ADR-001-modular-monolith-single-db.md), [ADR-002](ADR-002-ansible-vm-deploy.md), [B-01 auto build-and-push](../review/BACKLOG.md), [B-02 rollback path](../review/BACKLOG.md), [AUDIT D-1, D-3](../review/AUDIT.md), [ROADMAP Phase 0](../review/ROADMAP.md#phase-0--stabilize-delivery-p0)

## Context

This repo (`face-microservice`) is the deploy coordinator for three deployable services (`backend-service`, `face-ai-service`, `gui-app`) and one library (`email-service`). Today it pins each service as a git submodule. Each service repo already has its own `ci.yml` that builds and pushes `ghcr.io/mario-noobs/<service>:sha-<short>` on push to `main`/`develop`/`release/*` — so B-01 is effectively done at the service layer.

What is missing is **promotion**: nothing connects "service published a new image" to "dev environment runs the new image." The operator manually fires `deploy.yml` with a hand-pasted image tag (AUDIT D-1). There is also no rollback path (AUDIT D-3, B-02).

Observed submodule costs:
- Silent version drift (`gui-app` is pinned at `develop`, others at `main`).
- Two-step commits to promote (service repo commit, then submodule pointer bump here).
- Submodule pointer is a git SHA, not a deployable artifact (image tag). Requires mental mapping between the two at every debugging session.
- `docker-compose.yml` at repo root uses `build:` directives pointing into submodule checkouts — which only works locally, not on VMs. VMs run a separately-templated compose file. The duality is invisible in the repo.

The solo-developer constraint: optimize for **fast develop + quick shipping to a remote server**. Legibility of "what is running where" and "how do I roll back" matters more than team-scale controls.

## Options Considered

- **Option A (chosen): Orchestrator without submodules, `versions.yaml` manifest.** Replace `.gitmodules` entries for services with a per-environment tag manifest committed to this repo. `docker-compose.yml` uses `image:` with `${SERVICE}_TAG`; `docker-compose.override.yml` keeps `build:` for local hot-reload. A new `promote-dev.yml` workflow receives `repository_dispatch` from each service's `ci.yml`, updates `versions.yaml`, commits, and triggers the Ansible deploy. Git log of this repo = deploy history.
- **Option B: Keep submodules, only add promotion automation.** Minimum diff but compounds the pointer-vs-tag indirection and keeps the drift footguns.
- **Option C: Drop the orchestrator entirely.** Each service owns its deploy; a separate infra repo holds shared bits. Rejected — decentralization solves a team-scale coordination problem the user does not have. Losing cross-service E2E and a single view of "what runs where" is a net loss for a solo dev.
- **Option D: Monorepo.** Merge all services into one repo with path-filtered CI. Rejected — three language stacks, service CI is already working correctly, and migration cost is large with no proportional benefit.

## Decision

Keep the orchestrator repo, but retire submodules for the three services plus the email library. Source of truth for "what runs where" becomes a `versions.yaml` manifest committed here. Service repos stay independent with their existing CI. Promotion to `dev` is automated via `repository_dispatch`. Rollback is `git revert` on the manifest + redeploy. `ci-scripts/baseline` remains a submodule — it is the shared CI/Ansible library and is out of scope for this ADR.

## Consequences

**Positive:**
- `versions.yaml` makes system state legible in one file. Answers "what's on dev?" in a read.
- Git log on this repo is the deploy timeline. Rollback = `git revert` + redeploy.
- Promotion is automated. No manual image-tag paste (closes D-1 / B-01).
- Per-environment pinning is explicit (dev vs staging vs prod tags live side-by-side).
- Image tags are the deployable artifact — no indirection through a commit SHA.
- Removes the silent `develop` vs `main` drift currently present in the `gui-app` pointer.

**Negative / trade-offs:**
- Cannot browse service source from within the orchestrator checkout. Must use IDE jump-to-repo or clone services separately.
- Auto-commit on promotion creates commit noise on the orchestrator's default branch. Acceptable — those commits *are* the deploy log.
- `repository_dispatch` requires a PAT or GitHub App installation with write access to this repo; secret management adds one more thing.
- Cross-service schema coupling becomes visible in manifest diffs (a multi-service bump signals coordinated release) but the tooling does not enforce ordering — discipline still required.

**Neutral:**
- Local dev still works via `docker-compose.override.yml` with `build:` directives pointing at sibling checkouts. Developer clones services they want to modify; others pull pre-built images.
- E2E workflow switches from `build:` to `image:` — higher fidelity to what actually deploys.

## Rollback Plan

Restore `.gitmodules` entries and re-add submodule checkouts (trivial — the service repos still exist). Revert `docker-compose.yml` back to `build:` directives. Keep `versions.yaml` as a dead file or delete. Estimated cost: 1 hour. No production data is affected by this rollback.

## Success Metric

- Merge-to-dev wall-clock time under **5 minutes** (service CI + image push + promote workflow + Ansible deploy).
- Rollback wall-clock time under **2 minutes** (`git revert` + redeploy workflow).
- Zero manual image-tag paste operations on the `dev` environment.
- Manifest commits on this repo accurately reflect what each environment is running (verifiable by comparing `versions.yaml` to the VM's running image tags).
- Revisit trigger: a second developer joins and wants to iterate on services without cloning the orchestrator; or monorepo pressure grows as cross-service refactors become frequent.
