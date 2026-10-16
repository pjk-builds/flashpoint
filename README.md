# Flashpoint

Flashpoint is an execution-oriented flash-sale ordering service. It demonstrates how to protect scarce inventory when request traffic exceeds available stock and server capacity.

The first milestone has zero third-party runtime dependencies. It is an in-process correctness baseline that can be compiled, tested, load-tested, and demonstrated with Java and GNU Make.

## What the current milestone proves

- Inventory reservations are atomic under concurrent load; stock cannot become negative.
- `Idempotency-Key` makes client retries safe and detects keys reused with changed input.
- A token bucket absorbs short bursts and returns `429` beyond the configured request rate.
- A bounded worker pool and admission semaphore return `503` when capacity is saturated.
- Prometheus-format metrics expose request outcomes, latency, inventory, and in-flight work.
- A concurrent load generator reports throughput, status distribution, and p50/p95/p99 latency.

This is a single-process correctness baseline. State is intentionally in memory and is lost on restart until the durable-state milestone is implemented.

## Requirements

- Java 21 or newer
- GNU Make
- `curl` for the demo script

## Run locally

Start the service:

```bash
make run
```

In another terminal, check health:

```bash
curl -i http://localhost:8080/health
```

Expected response:

```http
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8

{"status":"UP"}
```

## Test

```bash
make test
```

The tests cover health, inventory, concurrent reservations, idempotency, rate limiting, and metrics formatting.

## Demo

With the service running in another terminal:

```bash
make demo
```

The demo restocks inventory, creates an order, replays the same request with the same idempotency key, and prints inventory and metrics.

## Run with Docker Compose

The container runs as a non-root user and binds to localhost by default:

```bash
make docker-build
make docker-up
curl http://localhost:8080/health
make docker-down
```

The current service stores inventory and idempotency state in memory. Recreating the container resets that state; persistent storage is part of the PostgreSQL milestone.

## Private Prometheus integration

Flashpoint exposes `/metrics` for Prometheus. Keep that endpoint private by placing the service and Prometheus on an internal Docker network. On the home server, copy `.env.example` to `.env` and then add this scrape job to the Prometheus configuration:

```yaml
- job_name: flashpoint
  metrics_path: /metrics
  static_configs:
    - targets: ["flashpoint:8080"]
```

Restart the monitoring stack and verify the `flashpoint` target is `UP`. Prometheus should reach the service through Docker DNS; no public port is required.

## Load testing

Start the service first, then run:

```bash
make load
```

The default scenario sends 10,000 order requests at concurrency 100. Run a custom scenario with:

```bash
java -cp build/main:build/test \
  io.quorumforge.flashpoint.LoadGenerator 50000 250 http://localhost:8080
```

Record the machine specification, Java version, configuration, duration, status distribution, and latency percentiles with every result. Performance numbers without an environment description are not reproducible evidence.

## API

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/health` | Liveness check |
| `GET` | `/inventory` | Current stock snapshot |
| `PUT` | `/inventory` | Add stock for an SKU |
| `POST` | `/orders` | Atomically attempt an order |
| `GET` | `/metrics` | Prometheus text exposition |

Example inventory setup:

```bash
curl -X PUT http://localhost:8080/inventory \
  -H 'Content-Type: application/json' \
  -d '{"sku":"console","quantity":10000}'
```

Example order:

```bash
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: customer-42-attempt-1' \
  -d '{"sku":"console","quantity":1}'
```

Order responses use `201` for accepted orders, `409` for sold-out inventory or idempotency conflicts, `429` for rate limiting, and `503` when concurrency is saturated.

## Configuration

| Variable | Default | Meaning |
|---|---:|---|
| `PORT` | `8080` | HTTP listen port |
| `RATE_LIMIT_RPS` | `5000` | Token refill rate per second |
| `RATE_LIMIT_BURST` | `1000` | Maximum accumulated burst tokens |
| `MAX_IN_FLIGHT` | `64` | Maximum admitted order requests |

For example:

```bash
RATE_LIMIT_RPS=500 RATE_LIMIT_BURST=100 MAX_IN_FLIGHT=16 make run
```

## Correctness contract

Within one running process:

1. Every unique idempotency key produces exactly one immutable order result.
2. A retry with the same key and payload returns the original order and consumes no additional stock.
3. A retry with a changed payload is rejected.
4. Successful reservations never exceed stocked inventory.
5. Overload is explicit; requests are rejected rather than queued without bound.

## Roadmap

The staged roadmap is documented in [`docs/roadmap.md`](docs/roadmap.md). The next milestone introduces durable state with PostgreSQL while preserving these domain invariants.
