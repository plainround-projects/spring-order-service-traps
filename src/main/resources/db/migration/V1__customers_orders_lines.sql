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
