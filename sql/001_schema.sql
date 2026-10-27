-- 001_schema.sql: the complete DDL of the order service in one script.
-- It is the three Flyway migrations (V1, V2, V3) concatenated; SchemaMirrorIT proves it builds the same schema.

-- ---- V1__customers_orders_lines.sql ----
-- Customers, orders and order lines. "order" is a reserved word, so the orders table is orders_table.
CREATE TABLE customers (
    id   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(100) NOT NULL CHECK (length(btrim(name)) > 0)
);
COMMENT ON TABLE customers IS 'People who place orders.';

CREATE TABLE orders_table (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id bigint      NOT NULL REFERENCES customers (id),
    placed_at   timestamptz NOT NULL
);
COMMENT ON TABLE orders_table IS 'One row per checkout that committed.';
-- The listing query filters by customer; without this index it scans every order.
CREATE INDEX orders_table_customer_id_idx ON orders_table (customer_id);

CREATE TABLE order_lines (
    id       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id bigint      NOT NULL REFERENCES orders_table (id) ON DELETE CASCADE,
    sku      varchar(64) NOT NULL CHECK (length(btrim(sku)) > 0),
    quantity integer     NOT NULL CHECK (quantity BETWEEN 1 AND 1000),
    UNIQUE (order_id, sku)
);
COMMENT ON TABLE order_lines IS 'The lines of an order; a SKU appears once per order.';
-- The fetch join reads all lines of a set of orders by order_id.
CREATE INDEX order_lines_order_id_idx ON order_lines (order_id);

-- ---- V2__stock_items.sql ----
-- Units on hand per SKU. "version" is the optimistic-locking column: two checkouts racing for the last unit
-- cannot both win, because the second UPDATE finds a different version and matches no row.
CREATE TABLE stock_items (
    sku       varchar(64) PRIMARY KEY CHECK (length(btrim(sku)) > 0),
    available integer NOT NULL CHECK (available >= 0),
    version   bigint  NOT NULL DEFAULT 0
);
COMMENT ON TABLE stock_items IS 'Units on hand per SKU, guarded by an optimistic-lock version.';

-- ---- V3__audit_records.sql ----
-- The audit trail. Rows are written in their own transaction (REQUIRES_NEW) so a failed checkout cannot erase them.
CREATE TABLE audit_records (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    action      varchar(64)  NOT NULL,
    detail      varchar(500) NOT NULL,
    recorded_at timestamptz  NOT NULL
);
COMMENT ON TABLE audit_records IS 'Append-only record of what was attempted; never holds secrets or personal data beyond ids.';
CREATE INDEX audit_records_recorded_at_idx ON audit_records (recorded_at DESC);

