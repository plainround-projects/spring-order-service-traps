#!/usr/bin/env bash
# The lesson's scenario through the HTTP API: checkout, the order list, a checkout that fails halfway, and the
# audit trail. The three traps themselves are proved by the tests (`make traps`).
# Usage: scripts/scenario.sh [base-url]   (default http://localhost:18102)
set -euo pipefail

BASE="${1:-http://localhost:${APP_PORT:-18102}}"
JSON='Content-Type: application/json'
heading() { printf '\n== %s\n' "$1"; }

heading "Set up: one customer and two SKUs (sku-keyboard: 5 units, sku-monitor: 0 units)"
CUSTOMER="$(curl -fsS -X POST "$BASE/customers" -H "$JSON" -d '{"name":"Ada Example"}')"
echo "$CUSTOMER" | jq -c .
ID="$(echo "$CUSTOMER" | jq -r .id)"
curl -fsS -X PUT "$BASE/stock/sku-keyboard" -H "$JSON" -d '{"available":5}' | jq -c .
curl -fsS -X PUT "$BASE/stock/sku-monitor" -H "$JSON" -d '{"available":0}' | jq -c .

heading "Checkout: two keyboards -> 201, the order with its lines"
curl -fsS -X POST "$BASE/customers/$ID/orders" -H "$JSON" -d '{"lines":[{"sku":"sku-keyboard","quantity":2}]}' | jq -c .

heading "The customer's orders with their lines (one query for any number of orders, see make traps)"
curl -fsS "$BASE/customers/$ID/orders" | jq -c .

heading "Trap 2 fixed: a checkout whose second line is out of stock is 409 and rolls back as a whole"
curl -sS -i -X POST "$BASE/customers/$ID/orders" -H "$JSON" \
  -d '{"lines":[{"sku":"sku-keyboard","quantity":1},{"sku":"sku-monitor","quantity":1}]}' |
  tr -d '\r' | grep -E '^(HTTP|Content-Type)|^\{' | sed 's/^{/body: {/'
echo "orders still stored: $(curl -fsS "$BASE/customers/$ID/orders" | jq length)   (1, the failed one left nothing)"
echo "sku-keyboard stock:  $(curl -fsS "$BASE/stock/sku-keyboard" | jq .available)   (3, the failed one gave its unit back)"

heading "Trap 3 fixed: both attempts, including the failed one, are in the audit trail"
curl -fsS "$BASE/audit?limit=5" | jq -c '.[] | {action, detail}'

heading "Validation, errors and health"
curl -sS -X POST "$BASE/customers" -H "$JSON" -d '{"name":""}' | jq -c .
curl -sS "$BASE/customers/999999/orders" | jq -c .
curl -fsS "$BASE/actuator/health" | jq -c .
