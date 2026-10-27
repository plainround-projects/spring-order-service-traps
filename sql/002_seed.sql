-- 002_seed.sql: the data the lesson's measurements use. Safe to run once on an empty schema.
-- Two customers; Ada has 20 orders of 3 lines each (the size of the lesson's N+1 test), Grace has none.
INSERT INTO customers (name) VALUES ('Ada Example'), ('Grace Example');

INSERT INTO stock_items (sku, available) VALUES
    ('sku-keyboard', 50),
    ('sku-mouse', 80),
    ('sku-monitor', 5);

INSERT INTO orders_table (customer_id, placed_at)
SELECT 1, timestamptz '2026-10-27 00:00:00+00' + (n || ' hours')::interval
FROM generate_series(1, 20) AS n;

INSERT INTO order_lines (order_id, sku, quantity)
SELECT o.id, s.sku, 1 + (o.id % 3)::int
FROM orders_table o
CROSS JOIN (VALUES ('sku-keyboard'), ('sku-mouse'), ('sku-monitor')) AS s(sku);

INSERT INTO audit_records (action, detail, recorded_at) VALUES
    ('CHECKOUT_ATTEMPT', 'customer 1, 3 line(s)', timestamptz '2026-10-27 01:00:00+00');
