# ADR-002: Ansible for VM Deployment

- **Status**: Accepted
- **Date**: 2026-04-15
- **Deciders**: Mario Bui (solo)
- **Related**: [AUDIT D-1, D-2, D-3, D-4](../review/AUDIT.md), [B-01 auto build-and-push](../review/BACKLOG.md), [B-02 rollback path](../review/BACKLOG.md), [B-03 fast redeploy](../review/BACKLOG.md), commit `457d128`

## Context

Deployment uses Ansible playbooks driven by `ci-scripts/Makefile` (`make deploy-dev`, `deploy-staging`, `deploy-prod`). Shared infra lives in the `ci-scripts/baseline/` git submodule. Inventories are per-environment at `ci-scripts/ansible/inventories/{dev,staging,prod}/`. Kubernetes support was **explicitly removed** in commit `457d128` ("simplify CI/CD to Ansible-only, remove all K8s config") after init-job and overlay issues proved too costly for a solo developer. Current pain points: no automated image build-and-push (D-1), deploys are slow full-rebuilds rather than `docker pull` + restart (D-2), no rollback path (D-3), no staged rollout (D-4).

## Options Considered

- **Option A (chosen): Ansible playbooks over SSH to VMs.** Simple, VMs already provisioned, operator knows Ansible. Addresses current scale (3 services, 1 developer).
- **Option B: Kubernetes.** Rejected and recently removed — operational overhead (manifests, overlays, init-job ordering) exceeded benefits at this scale.
- **Option C: Nomad / Docker Swarm.** Rejected — another orchestrator to learn with no clear advantage over Ansible for the current topology.
- **Option D: Managed PaaS (Fly.io, Render).** Rejected — project is tied to existing on-prem/VM infrastructure.

## Decision

Use Ansible as the sole deployment mechanism for all environments. Prioritize B-01 (auto build-and-push), B-02 (`make deploy-previous` rollback), and B-03 (replace full-deploy with `docker pull` + `systemctl restart`) to address audit findings D-1 through D-3 before considering any orchestrator.

## Consequences

**Positive:** One tool to learn and maintain. SSH-based deploys need no control plane. K8s removal cut hundreds of lines of overlay/manifest config. Local dev stays on `docker-compose` with no drift from prod topology.

**Negative / trade-offs:** No horizontal autoscaling. Rollback is manual until B-02 lands. Deploys are slow until B-03 lands. No canary/staged rollout (D-4) without custom scripting.

**Neutral:** The `ci-scripts/baseline/` submodule retains K8s artifact history, so the path back exists if needed.

## Rollback Plan

Supersede this ADR if we move to a container orchestrator. K8s manifests can be restored from `baseline/` git history. Estimated cost to re-adopt K8s: 1–2 weeks (manifests exist, but init-job and overlay issues must be re-solved).

## Success Metric

- Deploy-to-dev time **< 5 min** (ROADMAP Phase 0 target).
- Rollback time **< 2 min** once B-02 lands.
- Revisit trigger: service count exceeds 5, or horizontal autoscaling becomes a requirement, or ops burden exceeds 2 hours/week.
