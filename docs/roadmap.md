# Engineering roadmap

Each milestone introduces one distributed-systems concern and a test that proves its guarantees.

## M1 — Executable correctness baseline — current

- Single-process HTTP service
- Atomic stock reservations
- Idempotent order placement
- Rate limiting and bounded concurrency
- Prometheus metrics
- Contention tests and load generator
- Repeatable local demo

## M2 — Durable state

- PostgreSQL inventory and orders
- Conditional inventory update (`available >= requested`)
- Unique idempotency-key constraint
- Transactional outbox for reliable event publication
- Restart and transaction-contention integration tests

## M3 — Request-storm protection

- Redis-backed distributed rate limiting
- Per-customer and per-SKU limits
- Hot-SKU admission queues with bounded depth
- Retry budgets, jitter, and explicit `Retry-After`
- Load profiles for ramp, spike, soak, and coordinated retry storms

## M4 — Asynchronous fulfillment

- Kafka or NATS order events
- Payment and fulfillment workers
- At-least-once delivery with idempotent consumers
- Dead-letter handling and replay tooling
- Failure injection for worker, broker, and database outages

## M5 — Operability and horizontal scale

- OpenTelemetry traces and Grafana dashboards
- SLOs for availability and p99 latency
- Docker Compose development environment
- Multi-instance correctness tests
- Kubernetes manifests, readiness checks, and graceful shutdown

## Decisions to document as they arise

- Why inventory is strongly consistent while catalog reads may be eventually consistent
- At-least-once delivery plus idempotency instead of exactly-once claims
- Fail-open versus fail-closed rate limiting
- Database partition key and hot-key mitigation
- Queueing versus load shedding under overload
