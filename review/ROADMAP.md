# Roadmap — `face-microservice`

**Framework**: [LBC Framework](../../claude-exp/lbc-framework/) — phases aligned to SDLC workflow in [TEAM_DYNAMICS.md](../../claude-exp/lbc-framework/TEAM_DYNAMICS.md).
**Inputs**: [AUDIT.md](AUDIT.md), [BACKLOG.md](BACKLOG.md).
**Mode**: solo — one role at a time, hats switched explicitly (PO → Architect → Developer → QA → DevOps).

---

## Operating Principles for Solo Work

- **Wear one hat per session.** Before each session, say out loud which agent role you're playing.
- **DoR/DoD still apply** ([CHECKLISTS.md](../../claude-exp/lbc-framework/CHECKLISTS.md)) — you are your own reviewer.
- **ADRs are non-negotiable** from now on. Every load-bearing choice → 1 ADR.
- **Every ticket links to a backlog ID** (B-01…B-23) so progress is traceable.

---

## Phase 0 — Stabilize Delivery (P0)

**Goal**: Push-button, safe, auditable deploys. Never type `image_tag` by hand again.
**SDLC focus**: Release + Observe.
**Backlog items**: B-01, B-02, B-03, B-06, B-09, B-21
**Exit criteria**:
- [ ] Merge to `main` auto-builds & pushes an image per service.
- [ ] `make deploy-dev` pulls the latest tag (no manual input).
- [ ] `make deploy-previous` rolls back in < 2 minutes.
- [ ] Trivy runs on every PR; critical CVEs block merge.
- [ ] face-ai-service tests run on every PR that touches `face-ai-service/**`.
- [ ] Secrets live in exactly one place.

**Success signal**: You deploy to dev in < 5 minutes and can roll back in < 2.

---

## Phase 1 — See What's Actually Happening (P1)

**Goal**: Observability + contracts. You can't prioritize perf work without data, and you can't expose a mobile API without a contract.
**SDLC focus**: Observe + Refinement.
**Backlog items**: B-04, B-05, B-11, B-12, B-13, B-16, B-20, B-23
**Exit criteria**:
- [ ] Prometheus + Grafana deployed; RED dashboard for backend and face-ai-service.
- [ ] OTEL traces flow backend → face-ai-service (and to a collector).
- [ ] k6 baseline scripts for login, face-register, face-recognize; numbers committed to repo.
- [ ] OpenAPI 3.0 spec auto-published on each build; versioning policy ADR written.
- [ ] Initial SLOs written (login p95, face-recognize p95, 99.9% availability).
- [ ] ADR directory exists with ADR-001 (monolith+single DB), ADR-002 (Ansible deploy), ADR-003 (audit via RabbitMQ), ADR-004 (API versioning).
- [ ] Audit pipeline emits per-endpoint adoption counters; weekly report committed.

**Success signal**: You can answer "is login slow?" with a graph, not a feeling.

---

## Phase 2 — Enforce the Architecture You Already Chose (P2)

**Goal**: Prevent the monolith from decaying. Document seams before they matter.
**SDLC focus**: Build + Review.
**Backlog items**: B-07, B-08, B-10, B-14, B-15, B-22
**Exit criteria**:
- [ ] ArchUnit test enforces "no cross-module repo imports"; CI fails if violated.
- [ ] JaCoCo raised to 50% global / 70% auth; trending upward.
- [ ] Nightly E2E against `dev` passes for 7 consecutive nights.
- [ ] Feature flag library in place; one flag shipped end-to-end (even just a toggle).
- [ ] Module seam plan written for `face/` module: what would it take to extract? (No extraction — just the plan.)
- [ ] Design system inventory in `gui-app`: tokens, shared components, 5-state coverage per screen.

**Success signal**: You can add a new feature behind a flag and roll it out to yourself first.

---

## Phase 3 — Validated Bets (P3)

**Goal**: *Now* you can answer "perf vs OCR" with evidence.
**SDLC focus**: Discovery → Build → Release (flagged, measured).
**Backlog items**: B-17 or B-18 (decision below), B-19

### Decision gate — perf vs OCR

Run this checklist at the start of P3:

1. Look at the k6 baseline and Prometheus p95s from P1.
2. If login or face-recognize p95 exceeds your SLO → **B-18 (Perf) wins**. You have a real problem.
3. If SLOs are green → **B-17 (OCR) wins**, because:
   - Higher ICE score (7.0 vs 4.0 today; likely higher still with real data).
   - Reuses the face-ai-service pattern → low architectural risk.
   - Ships behind a feature flag (from P2) → low release risk.
   - Measured by the adoption counter (from P1) → stop rule is "if adoption < X% after 4 weeks, sunset."

### Mobile app (B-19)

- Only start *after* B-11 (OpenAPI) is firm and has been stable for at least 4 weeks.
- Start with a thin slice: login + 1 face-recognize flow. Don't port the admin panel.
- Treat it as its own Phase (P4) — don't mix with backend changes.

**Exit criteria for P3**:
- [ ] The chosen bet is live behind a flag in dev for at least 1 week before staging.
- [ ] Adoption metric + SLO dashboard show the bet's effect; stop rule either met or clearly not.

---

## Cadence & Rituals (solo version)

| Ritual | Cadence | Role you play |
|---|---|---|
| **Weekly review** | 30 min, Fridays | ProductOwner — read dashboards, update backlog scores, re-rank |
| **Refinement** | Before each ticket | PO + Designer + QA hats — write DoR for the ticket |
| **Code review** | On every PR to self | Developer + TechLead hats — don't merge your own PR same session you wrote it |
| **ADR writing** | When you make a load-bearing choice | Architect hat — write it *before* implementing, not after |
| **Incident/escape post-mortem** | When something breaks | QA hat — one page, what happened, what changed, what's now guarded |

---

## What This Roadmap Is *Not*

- Not a timeline with dates. Deliberately. You'll set those per-phase based on your own availability.
- Not a rewrite plan. The architecture is fine. The *delivery* is what hurts.
- Not a commitment to build OCR or optimize perf. It's a commitment to *earn* the right to decide.

---

## Phase-Out / Kill Criteria

Stop any phase item if:
- You've spent 2× the time you estimated and are not close.
- A higher-scoring item appears in the backlog.
- You discover the assumption the item was built on is wrong.

Update BACKLOG.md scores weekly based on what you learn. **The backlog is the product of the product.**
