#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://localhost:8080}"
sku="${SKU:-console}"
quantity="${QUANTITY:-100}"
order_key="demo-order-$(date +%s)"

printf '%s\n' "Restocking $quantity $sku units"
curl --fail-with-body -sS -X PUT "$base_url/inventory" \
  -H 'Content-Type: application/json' \
  -d "{\"sku\":\"$sku\",\"quantity\":$quantity}"
printf '\n\n'

printf '%s\n' "Creating an order"
curl --fail-with-body -sS -X POST "$base_url/orders" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $order_key" \
  -d "{\"sku\":\"$sku\",\"quantity\":1}"
printf '\n\n'

printf '%s\n' "Replaying the same order safely"
curl --fail-with-body -sS -X POST "$base_url/orders" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $order_key" \
  -d "{\"sku\":\"$sku\",\"quantity\":1}"
printf '\n\n'

printf '%s\n' "Current inventory"
curl --fail-with-body -sS "$base_url/inventory"
printf '\n\n'

printf '%s\n' "Prometheus metrics"
curl --fail-with-body -sS "$base_url/metrics"
