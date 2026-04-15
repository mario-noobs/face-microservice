# Backlog — ICE-Scored

**Source**: [AUDIT.md](AUDIT.md) findings + user-named bets (perf optimization, OCR service, mobile app).
**Scoring**: ICE from [METRICS.md](../../claude-exp/lbc-framework/METRICS.md) — `(Impact × Confidence) ÷ Effort`, all on 1–10.
**Ordering**: Risk-first within each ICE band (per `PRINCIPLES.md` → Product).

---

## Scoring Notes (solo-project calibration)

- **Impact**: how much it removes friction *for you* (not a user base you don't have yet).
- **Confidence**: how much evidence you have that it'll work. Low for things with no baseline data.
- **Effort**: relative; 1 = afternoon, 10 = month+.

---

## Backlog

| ID | Item | Source | I | C | E | **Score** | Notes |
|---|---|---|---|---|---|---|---|
| B-01 | Auto build-and-push images on merge to `main` (one workflow per service, path-filtered) | D-1 | 9 | 9 | 3 | **27.0** | Unblocks every deploy. Lowest-cost highest-value. |
| B-02 | Rollback path: keep last N image tags, `make deploy-previous` target | D-3 | 9 | 8 | 3 | **24.0** | Required before any prod push is sane. |
| B-03 | Replace Ansible "full deploy" with `docker pull` + `systemctl restart` per service | D-2 | 8 | 8 | 4 | **16.0** | Removes long-deploy pain; keeps Ansible for infra-level changes only. |
| B-04 | Add Prometheus + Grafana + basic RED dashboards (latency/errors/traffic) to docker-compose and deploy | D-5 | 9 | 8 | 4 | **18.0** | Needed to *answer* the perf question with data. |
| B-05 | Add OpenTelemetry traces: backend → face-ai-service | D-5, E-5 | 7 | 7 | 4 | **12.3** | Makes cross-service issues debuggable. |
| B-06 | Wire face-ai-service tests into a CI workflow on PRs that touch `face-ai-service/**` | Q-3 | 8 | 9 | 2 | **36.0** | Trivially cheap, big safety win. |
| B-07 | Raise JaCoCo global threshold 30% → 50%; auth packages 50% → 70% | Q-1 | 6 | 8 | 3 | **16.0** | Quality floor; ratchet up carefully. |
| B-08 | Nightly E2E against `staging` (or `dev`) environment | Q-2 | 6 | 7 | 3 | **14.0** | Catches config drift that PR-only E2E misses. |
| B-09 | Trivy on every PR (not just weekly) | D-7 | 7 | 9 | 2 | **31.5** | One-line workflow change. |
| B-10 | Introduce feature flag library (e.g. Unleash, GrowthBook self-hosted, or simple DB-backed flags) | P-3 | 8 | 7 | 5 | **11.2** | Prereq for safe OCR / perf experiments. |
| B-11 | OpenAPI 3.0 spec auto-generated from backend; publish to repo; used by future mobile client | U-3, E-4 | 8 | 9 | 3 | **24.0** | Springdoc. Solves mobile contract + backend-↔-service clarity. |
| B-12 | Load/perf baseline with k6 (login, face-recognize, face-register) | Q-4 | 9 | 9 | 4 | **20.3** | **Prereq to even consider "optimize perf" as a bet.** |
| B-13 | ADR directory + first 3 ADRs: (a) modular monolith + single DB, (b) ansible deploy, (c) audit via RabbitMQ | E-3, C-1 | 5 | 10 | 2 | **25.0** | Cheap future-proofing. |
| B-14 | ArchUnit test enforcing "no cross-module repo imports" rule | E-2 | 6 | 8 | 2 | **24.0** | Prevents the monolith from decaying into a ball of mud. |
| B-15 | Module seam plan: pick 1 module (candidate: `face/`) and document how it would be extracted (schema split, event contracts) | E-1 | 7 | 6 | 3 | **14.0** | Document now, extract only if justified later. |
| B-16 | Define SLOs for critical paths: login p95, face-recognize p95, 99.9% availability | D-6 | 7 | 7 | 2 | **24.5** | Gate future releases. |
| B-17 | **OCR service (new AI microservice, mirrors face-ai-service pattern)** | User bet | 8 | 7 | 8 | **7.0** | High impact if users care. Confidence only 7 without validated user demand. Effort is real. |
| B-18 | **Backend perf optimization (specific hot path)** | User bet | 6 | 4 | 6 | **4.0** | Confidence LOW until B-12 gives you a baseline. Don't do this yet. |
| B-19 | Mobile app scaffolding (React Native / Flutter) consuming `/api/v1/` | User ambition | 7 | 6 | 9 | **4.7** | Effort is high; do after B-11 (OpenAPI) so contract is firm. |
| B-20 | Product metrics: per-endpoint adoption counter in audit pipeline, weekly report | P-1 | 6 | 8 | 3 | **16.0** | You already have an audit pipeline → near-free. |
| B-21 | Consolidate secrets: pick one source of truth (Ansible Vault OR top-level `vault/`), remove the other | E-6 | 5 | 8 | 2 | **20.0** | Easy cleanup. |
| B-22 | Design system audit in `gui-app`: tokens, shared components, 5-state coverage per screen | U-1, U-2 | 6 | 6 | 5 | **7.2** | Do before mobile (B-19) so shared component principles travel. |
| B-23 | API versioning policy doc: deprecation window, breaking-change process | E-4 | 5 | 8 | 1 | **40.0** | 1 page of doc, huge leverage. |

---

## Top 10 by Score

1. **B-23** (40.0) — API versioning policy doc
2. **B-06** (36.0) — face-ai-service CI on PR
3. **B-09** (31.5) — Trivy per-PR
4. **B-01** (27.0) — Auto build-and-push on main
5. **B-13** (25.0) — ADR directory + first 3 ADRs
6. **B-16** (24.5) — SLOs for critical paths
7. **B-02** (24.0) — Rollback path
8. **B-11** (24.0) — OpenAPI spec published
9. **B-14** (24.0) — ArchUnit enforcement
10. **B-12** (20.3) — Load/perf baseline

## Direction Answer: Perf vs OCR

Using ICE explicitly:

- **B-18 (Perf)** scores **4.0** — confidence is only 4 because you have no baseline (Q-4). Optimizing without data is guessing.
- **B-17 (OCR)** scores **7.0** — higher impact and higher confidence (clear feature, clear pattern to reuse from face-ai-service), but effort is 8.

**Tiebreaker** (from `METRICS.md`): "choose the item that reduces risk or validates the riskiest assumption."

- **Riskiest assumption for OCR**: "users will actually use it."
- **Riskiest assumption for Perf**: "current perf is actually a problem."

**Neither should be next.** Both need to be unblocked first by cheaper work on this list. Specifically:

- Before OCR: B-10 (feature flags) + B-11 (OpenAPI) + B-20 (adoption metrics) — so you can ship OCR gated + measure whether anyone uses it.
- Before Perf: B-04 (Prometheus dashboards) + B-12 (k6 baseline) — so you *know* whether perf is a problem and by how much.

See [ROADMAP.md](ROADMAP.md) for the phased sequencing.
