# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) for the face-microservice project. Each ADR documents a load-bearing technical decision with context, options considered, tradeoffs, a rollback plan, and a success metric. To add a new ADR, create `ADR-NNN-kebab-title.md` following the template in any existing ADR, and update the table below.

| ID  | Title                                          | Status   | Date       | File                                                        |
|-----|------------------------------------------------|----------|------------|-------------------------------------------------------------|
| 001 | Modular Monolith with Single MySQL Database    | Accepted | 2026-04-15 | [ADR-001](ADR-001-modular-monolith-single-db.md)            |
| 002 | Ansible for VM Deployment                      | Accepted | 2026-04-15 | [ADR-002](ADR-002-ansible-vm-deploy.md)                     |
| 003 | Audit Pipeline via RabbitMQ                    | Accepted | 2026-04-15 | [ADR-003](ADR-003-audit-pipeline-rabbitmq.md)               |
