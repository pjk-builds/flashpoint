# Flashpoint

Flashpoint will become a high-concurrency order-admission and inventory-reservation service for limited-inventory sales.

The project is being developed incrementally through focused, executable pull requests. The current version provides an HTTP health endpoint; business behavior will follow in later changes.

## Requirements

- Java 21 or newer
- GNU Make

## Run

```bash
make run
```

In another terminal:

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
