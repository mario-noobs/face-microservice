# ADR-003: Audit Pipeline via RabbitMQ with MySQL + Elasticsearch + Email Sinks

- **Status**: Accepted
- **Date**: 2026-04-15
- **Deciders**: Mario Bui (solo)
- **Related**: [ADR-001](ADR-001-modular-monolith-single-db.md), [ROADMAP Phase 1](../review/ROADMAP.md#phase-1--see-whats-actually-happening-p1)

## Context

Every HTTP request passes through `AuditLoggingFilter`, which delegates to `AuditEventPublisher` to publish an event to RabbitMQ. The broker fans out to three consumers: `AuditLogConsumer` (MySQL — durable record), `AuditSearchConsumer` (Elasticsearch — full-text search), and `AlertConsumer` (email alerts for critical events). Configuration in `RabbitMQConfig` declares a durable fanout exchange (`audit.exchange`), three durable queues (`audit.persist.queue`, `audit.search.queue`, `audit.alert.queue`), and a dead-letter exchange (`audit.dlx` → `audit.dlq`). If RabbitMQ is unreachable, `AuditEventPublisher` falls back to a **synchronous direct write** to MySQL via `AuditService.saveAuditLog()`, so the durable record is never lost — at the cost of coupling the request to the DB call. Actor identity comes from `request.getAttribute("_audit_user")` set by `JwtAuthenticationFilter`. Action semantics are mapped by `AuditActionMapper` (e.g., `POST /api/v1/user/authenticate` → `auth:login`).

## Options Considered

- **Option A (chosen): RabbitMQ fanout + 3 consumers + synchronous DB fallback.** Decouples request latency from ES and SMTP. Adding a new sink means binding one more queue. DB fallback guarantees durability even during broker outages.
- **Option B: Synchronous writes to all three sinks in the filter.** Rejected — couples request latency to ES indexing and SMTP delivery; partial failures leave inconsistent state.
- **Option C: Kafka.** Rejected — operational weight (ZooKeeper/KRaft, partition management) is unjustified. RabbitMQ is the only broker in the stack; audit is currently its sole consumer, which is acceptable at this scale.
- **Option D: Structured logs + Filebeat/Fluentd to ES only.** Rejected — no durable MySQL row of record, no programmatic alerting, log pipeline reliability is harder to reason about than a broker with DLQ.

## Decision

Keep the RabbitMQ fanout pipeline with three consumers and the synchronous MySQL fallback. Monitor DLQ depth and fallback-write count as operational health signals.

## Consequences

**Positive:** Request path is decoupled from ES and SMTP. Fanout pattern makes adding sinks trivial. DLQ captures poison messages for inspection. Fallback guarantees MySQL durability.

**Negative / trade-offs:** RabbitMQ is a runtime dependency for the async path (ES search + email alerts degrade if the broker is down). The synchronous fallback re-couples the request to MySQL on broker failure, adding latency. RabbitMQ currently serves only the audit pipeline — it is infrastructure carried for one use case.

**Neutral:** Elasticsearch is an eventually-consistent view; brief lag between action and searchability is acceptable for audit use cases.

## Rollback Plan

Remove RabbitMQ and make `AuditEventPublisher` always use the synchronous DB path (the fallback code already exists). ES indexing and email alerts would need to be re-implemented synchronously or via a polling/CDC approach. Estimated cost: 1–2 days for removal, 1 week to restore ES + alerts via an alternative path.

## Success Metric

- Zero audit-event loss over a rolling 30 days: DLQ depth stays at **0**; fallback-write count stays near **0** outside known broker outages.
- Revisit trigger: audit volume exceeds single-node RabbitMQ throughput, or per-tenant partitioning is needed (then evaluate Kafka).
