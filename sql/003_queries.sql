-- 003_queries.sql: the queries the lesson talks about, each with a line on what it shows.
-- Run with `make queries`.

-- 1. Trap 1, the naive listing, part one: one statement for a customer's orders.
SELECT id, placed_at FROM orders_table WHERE customer_id = 1 ORDER BY id;

-- 2. Trap 1, part two: this statement runs once PER ORDER when the lines are loaded lazily (20 orders, 20 runs).
SELECT id, sku, quantity FROM order_lines WHERE order_id = 1 ORDER BY id;

-- 3. The fix: what the entity graph sends instead, one join for all orders and all their lines.
SELECT o.id AS order_id, o.placed_at, l.id AS line_id, l.sku, l.quantity
FROM orders_table o
LEFT JOIN order_lines l ON l.order_id = o.id
WHERE o.customer_id = 1
ORDER BY o.id, l.id;

-- 4. The shape of the data: orders and lines per customer.
SELECT c.name, count(DISTINCT o.id) AS orders, count(l.id) AS lines
FROM customers c
LEFT JOIN orders_table o ON o.customer_id = c.id
LEFT JOIN order_lines l ON l.order_id = o.id
GROUP BY c.name
ORDER BY c.name;

-- 5. Trap 2, the symptom to look for: an order without any line is a half-saved checkout. Expect zero rows.
SELECT o.id FROM orders_table o LEFT JOIN order_lines l ON l.order_id = o.id WHERE l.id IS NULL;

-- 6. Stock never goes negative (the CHECK constraint and the optimistic version guard it).
SELECT sku, available, version FROM stock_items ORDER BY sku;

-- 7. Trap 3, the audit trail: attempts that failed are still here because they committed in their own transaction.
SELECT id, action, detail, recorded_at FROM audit_records ORDER BY id DESC LIMIT 10;

-- 8. The indexes the listing relies on.
SELECT indexname, indexdef FROM pg_indexes WHERE schemaname = 'public' AND indexname LIKE '%_idx' ORDER BY indexname;
