# Product Audit — `face-microservice`

**Date**: 2026-04-15
**Scored against**: [LBC Framework — PRINCIPLES.md](../../claude-exp/lbc-framework/PRINCIPLES.md)
**Scope**: backend-service (Java), face-ai-service (Python), gui-app (React), CI/CD, infra.

Severity: **H** = high (blocks scale/safety), **M** = medium (slows delivery), **L** = low (polish).

---

## Product

| # | Finding | Evidence | Severity |
|---|---|---|---|
| P-1 | No measurable outcome metrics attached to any feature (no adoption %, no SLO). | README features list is capability-based, not metric-based. | H |
| P-2 | No product backlog with ranked bets — direction ("perf vs OCR") is unresolved because there's no scoring mechanism in place. | User-reported; no `BACKLOG.md` or equivalent. | H |
| P-3 | Feature flag mechanism absent → no way to ship behind a flag or A/B. | No flag config in backend or frontend. | M |
| P-4 | No user segment definition (who uses face recognition, for what job-to-be-done). | README describes features, not users. | M |

## UX / Design

| # | Finding | Evidence | Severity |
|---|---|---|---|
| U-1 | No design system named; component library unclear. | `gui-app/` has React + Tailwind but no Storybook / design tokens doc. | M |
| U-2 | Five-state coverage (default/loading/empty/error/success) not verified per screen. | No explicit state inventory. | M |
| U-3 | Mobile extension ambition declared but web UI is not yet responsive-first / API is not versioned beyond `/api/v1/`. | `/api/v1/*` endpoints present; no mobile contract doc. | M |

## Engineering

| # | Finding | Evidence | Severity |
|---|---|---|---|
| E-1 | **Single MySQL DB for a modular monolith** — module boundaries exist in code but not in data. Ripping a module out later (e.g., `face/` into its own service) will require a data migration project. | README architecture diagram; `init.sql` seeds one DB. | H |
| E-2 | Module communication rule declared ("interfaces only, never cross-module repo imports") but not enforced by tooling. | `CLAUDE.md:87`; no ArchUnit/Spotbugs rule. | M |
| E-3 | No ADR directory — architectural decisions live in `CLAUDE.md` prose, not versioned decisions with tradeoffs/rollback. | No `adr/` or `docs/adr/` folder. | M |
| E-4 | API versioning is path-based (`/api/v1/`) but no formal deprecation/evolution policy written down — critical before a mobile client starts consuming it. | No `API_VERSIONING.md`. | M |
| E-5 | Python AI service and Java backend share no contract schema (no OpenAPI export, no protobuf). Cross-service breakage risk. | `FACE_SERVICE_URL` env var; no schema file committed. | M |
| E-6 | Secrets management mixed: `vault/` folder + env vars + Ansible Vault. Source of truth unclear. | Top-level `vault/`; `.github/workflows/deploy.yml:39` uses `ANSIBLE_VAULT_PASSWORD`. | M |

## Quality

| # | Finding | Evidence | Severity |
|---|---|---|---|
| Q-1 | Global coverage threshold is only 30% (50% for auth packages) — well below where critical paths should be. | `CLAUDE.md:36`. | M |
| Q-2 | E2E runs on PR only; no nightly on `main` against deployed envs. | `.github/workflows/e2e.yml:3-5` (only `pull_request` + manual). | M |
| Q-3 | Python AI service has `tests/` but no workflow triggers — unclear if ever run in CI. | No `*.yml` referencing `face-ai-service/` tests. | H |
| Q-4 | No load/perf test suite visible → cannot answer the "optimize perf" question with data. | No k6/JMeter/locust config. | H |
| Q-5 | No API contract tests between backend ↔ Python AI service. | E-5 above; no Pact or equivalent. | M |

## Collaboration

| # | Finding | Evidence | Severity |
|---|---|---|---|
| C-1 | Solo project → decision record discipline is easy to skip, but **costs compound** when you bring in a collaborator or AI agent for changes. | No ADRs; no decision log. | M |
| C-2 | `CLAUDE.md` exists (good) but is prose reference, not a decision trail. | `CLAUDE.md` is current-state doc. | L |

## Delivery / Process

| # | Finding | Evidence | Severity |
|---|---|---|---|
| D-1 | **No auto build-and-push image workflow on merge to `main`.** `deploy.yml` requires the user to paste an `image_tag` manually — this is the "manually modify to make it work" pain. | `.github/workflows/deploy.yml:24-27`. | H |
| D-2 | Ansible deploy via `make deploy-*` is the single deploy path and user reports it's slow. Likely because it rebuilds/copies rather than `docker pull` + `systemctl restart`. | `ci-scripts/Makefile:18-25` calls `./scripts/deploy-dev.sh` → unknown internals; user pain reported. | H |
| D-3 | No rollback plan or previous-image-pin mechanism documented. | `deploy.yml` accepts any tag; no "deploy previous" path. | H |
| D-4 | No staged rollout / canary — all-or-nothing deploys. | Single Ansible target per env. | M |
| D-5 | No observability stack wired in. Fluent-bit configs exist but no metrics (Prometheus) or traces (OTEL) confirmed. | `logging/` has fluent-bit; no Prometheus in `docker-compose.yml` top level. | H |
| D-6 | No SLOs defined → cannot gate releases on error budget. | Not in any file. | M |
| D-7 | Security: Trivy runs weekly only — not on every PR. Critical CVEs can ship. | `.github/workflows/security-scan.yml:4-5` (cron only). | M |

---

## Severity Summary

- **High (H)**: 9 — P-1, P-2, E-1, Q-3, Q-4, D-1, D-2, D-3, D-5
- **Medium (M)**: 15
- **Low (L)**: 1

## Top 3 Takeaways

1. **Your biggest debt is delivery, not architecture.** D-1/D-2/D-3/D-5 (no auto-build, slow deploy, no rollback, no observability) are the real "messy" — they're the reason releases hurt.
2. **Your "perf vs OCR" direction question cannot be answered rigorously today** — there's no perf data (Q-4), no adoption metric (P-1), no SLO (D-6). Any answer is a guess.
3. **Architecture is OK for now.** Modular monolith + one DB is the correct default for a solo project. Don't rewrite it — but do add the **enforcement** (E-2) and a **seam plan** (E-1) so breaking it apart later is cheap.
