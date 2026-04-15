# ADR-001: Modular Monolith with Single MySQL Database

- **Status**: Accepted
- **Date**: 2026-04-15
- **Deciders**: Mario Bui (solo)
- **Related**: [AUDIT E-1, E-2, E-3](../review/AUDIT.md), [B-14 ArchUnit enforcement](../review/BACKLOG.md), [B-15 face module seam plan](../review/BACKLOG.md), [ROADMAP Phase 2](../review/ROADMAP.md#phase-2--enforce-the-architecture-you-already-chose-p2)

## Context

The backend (`backend-service/`) is a Spring Boot modular monolith with eight feature packages (`auth/`, `users/`, `rbac/`, `face/`, `audit/`, `gateway/`, `common/`, `logging/`). All modules share a single MySQL database (`backend_db`) managed by Liquibase. A module communication rule exists in prose ("modules communicate through service interfaces only — never import another module's repository directly") but is not enforced by tooling (E-2). RBAC, user, and audit tables live in one schema; module boundaries exist only in code, not in data (E-1). The project is solo-developed with no production users yet.

## Options Considered

- **Option A (chosen): Modular monolith + single DB.** One deployment unit, one schema, ACID transactions across modules. Fast for a solo developer; minimal operational overhead.
- **Option B: Microservices from day 1.** Rejected — operational overhead (network, distributed transactions, per-service CI/CD) is unjustified with zero users and one developer.
- **Option C: Monolith + separate schema per module.** Rejected for now — adds migration tooling complexity without a forcing function. Kept as the migration path if extraction becomes necessary.

## Decision

Keep the modular monolith architecture with a single MySQL database. Enforce module boundaries in code via ArchUnit tests (B-14) before adding any new module. Document extraction cost for `face/` as a seam plan (B-15) so we know the price of splitting before we need to pay it.

## Consequences

**Positive:** Single deploy, simple transactions, one Liquibase changelog set, fast local dev via `docker-compose`. Module extraction remains possible because service-interface boundaries are maintained by convention (and soon by ArchUnit).

**Negative / trade-offs:** Ripping a module out later requires a data migration project (E-1). A single DB is a shared-fate component — schema mistakes affect all modules. Without ArchUnit (B-14), cross-module coupling can creep in silently.

**Neutral:** Liquibase per-env changelogs (`dev/`, `staging/`, `prod/`) already support divergent migration paths if schemas split later.

## Rollback Plan

Supersede this ADR with an extraction ADR when justified. Steps: (1) split schema using Liquibase migrations, (2) extract module into its own service + DB, (3) introduce inter-service calls (REST or async). Estimated cost: 2–4 weeks for `face/` module based on current coupling.

## Success Metric

- Cross-module repository imports stay at **0** (enforced by ArchUnit once B-14 lands).
- Extraction cost for `face/` remains under **2 weeks** as estimated in seam plan B-15.
- Revisit trigger: a second developer joins, or module count exceeds 10, or a module needs independent scaling.
