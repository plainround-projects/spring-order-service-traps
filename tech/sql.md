# SQL

## What it is

SQL (Structured Query Language) is the language you use to talk to a relational database. Statements fall into groups: **DDL** (data definition: `CREATE TABLE`, `CREATE INDEX`, the shape of the data), **DML** (data manipulation: `INSERT`, `UPDATE`, `DELETE`, the data itself) and queries (`SELECT`, reading it).

## Why this repository uses it

The template says every SQL statement a project runs is a file in `sql/`. The same statements are what Flyway runs at startup, and the queries are what Hibernate sends on your behalf. Reading the SQL shows what the traps look like at the database.

## Where it appears

| Path | Kind | What it holds |
| --- | --- | --- |
| `src/main/resources/db/migration/V1__...V3__...sql` | DDL | the schema, applied by Flyway |
| `sql/001_schema.sql` | DDL | the three migrations as one script |
| `sql/002_seed.sql` | DML | demo data: two customers, 20 orders with 3 lines each, stock |
| `sql/003_queries.sql` | queries | the lesson's queries, each with a comment |
| `sql/migrations/` | DDL | a mirror of the Flyway files |

## How it works underneath

1. The client sends a statement as text. Postgres parses it, plans how to run it, runs it, and returns rows or a count.
2. A table has **columns** with types. A **primary key** identifies each row. A **foreign key** says a column refers to another table's key and must match an existing row. A **`CHECK`** constraint rejects rows that break a rule. An **index** is a sorted structure that lets the database find rows without scanning the table.
3. A `SELECT ... FROM a JOIN b ON ...` combines rows from two tables where the condition matches. `LEFT JOIN` keeps rows of `a` that have no match.
4. `GROUP BY` collapses rows into groups for `count`, `sum` and similar.
5. `--` starts a comment to the end of the line.

## Walkthrough of this repository's use

```sql
CREATE TABLE customers (
    id   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(100) NOT NULL CHECK (length(btrim(name)) > 0)
);
COMMENT ON TABLE customers IS 'People who place orders.';
```

`CREATE TABLE customers (...)` defines a table. `id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY` is a 64-bit number the database assigns, and the primary key. `name varchar(100) NOT NULL CHECK (length(btrim(name)) > 0)`: text of at most 100 characters, never null, and `btrim` removes surrounding spaces so a blank name is rejected. `COMMENT ON TABLE` stores a description in the database.

```sql
CREATE TABLE orders_table (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id bigint      NOT NULL REFERENCES customers (id),
    placed_at   timestamptz NOT NULL
);
COMMENT ON TABLE orders_table IS 'One row per checkout that committed.';
-- The listing query filters by customer; without this index it scans every order.
CREATE INDEX orders_table_customer_id_idx ON orders_table (customer_id);
```

`orders_table` (not `order`, which is a reserved SQL word) has `customer_id bigint NOT NULL REFERENCES customers (id)`: a **foreign key**. `placed_at timestamptz` is a timestamp with time zone. The index on `customer_id` exists because the listing query filters by customer; without it Postgres would scan every order.

```sql
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
```

`ON DELETE CASCADE` deletes a line when its order is deleted. `CHECK (quantity BETWEEN 1 AND 1000)` and `UNIQUE (order_id, sku)` (a SKU appears once per order) repeat in the database the rules the Java code already enforces.

```sql
CREATE TABLE stock_items (
    sku       varchar(64) PRIMARY KEY CHECK (length(btrim(sku)) > 0),
    available integer NOT NULL CHECK (available >= 0),
    version   bigint  NOT NULL DEFAULT 0
);
COMMENT ON TABLE stock_items IS 'Units on hand per SKU, guarded by an optimistic-lock version.';
```

`sku varchar(64) PRIMARY KEY`, `available integer NOT NULL CHECK (available >= 0)` (stock can never go negative), and `version bigint NOT NULL DEFAULT 0`, the counter for optimistic locking (see `spring-data-jpa.md`).

```sql
INSERT INTO orders_table (customer_id, placed_at)
SELECT 1, timestamptz '2026-10-27 00:00:00+00' + (n || ' hours')::interval
FROM generate_series(1, 20) AS n;
```

`INSERT INTO ... SELECT` fills a table from a query. `generate_series(1, 20)` produces the numbers 1 to 20, so this creates 20 orders, one hour apart, for customer 1.

```sql
-- 3. The fix: what the entity graph sends instead, one join for all orders and all their lines.
SELECT o.id AS order_id, o.placed_at, l.id AS line_id, l.sku, l.quantity
FROM orders_table o
LEFT JOIN order_lines l ON l.order_id = o.id
WHERE o.customer_id = 1
ORDER BY o.id, l.id;
```

The statement the entity graph sends: `LEFT JOIN` returns each order once per line; the application groups the rows. One statement, however many orders.

**The rest of the schema, statement by statement.**

```sql
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
```

- `COMMENT ON TABLE ... IS '...'` stores a description inside the database. The `orders_table` and `customers` tables are described in the same way at the top of this file (quoted above).
- The statement `CREATE INDEX order_lines_order_id_idx ON order_lines (order_id)` builds an index on the foreign key column. PostgreSQL does not create indexes for foreign keys by itself, and the fetch join reads all lines of a set of orders by `order_id`, so without the index every listing would scan the whole `order_lines` table.

**Version 2, the stock table, in full:**

```sql
-- Units on hand per SKU. "version" is the optimistic-locking column: two checkouts racing for the last unit
-- cannot both win, because the second UPDATE finds a different version and matches no row.
CREATE TABLE stock_items (
    sku       varchar(64) PRIMARY KEY CHECK (length(btrim(sku)) > 0),
    available integer NOT NULL CHECK (available >= 0),
    version   bigint  NOT NULL DEFAULT 0
);
COMMENT ON TABLE stock_items IS 'Units on hand per SKU, guarded by an optimistic-lock version.';
```

- The two comment lines at the top use `--`. `CREATE TABLE stock_items (...)` has three columns. `sku varchar(64) PRIMARY KEY CHECK (length(btrim(sku)) > 0)`: the SKU is the primary key (unique, never null) and may not be blank. `available integer NOT NULL CHECK (available >= 0)`: whole number, required, never negative: the database itself refuses to oversell. `version bigint NOT NULL DEFAULT 0`: a counter that starts at 0 (`DEFAULT` is the value used when an insert gives none); Hibernate increments it on every update and uses it for optimistic locking (see `spring-data-jpa.md`).
- `COMMENT ON TABLE` documents the table.

**Version 3, the audit table, in full:**

```sql
-- The audit trail. Rows are written in their own transaction (REQUIRES_NEW) so a failed checkout cannot erase them.
CREATE TABLE audit_records (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    action      varchar(64)  NOT NULL,
    detail      varchar(500) NOT NULL,
    recorded_at timestamptz  NOT NULL
);
COMMENT ON TABLE audit_records IS 'Append-only record of what was attempted; never holds secrets or personal data beyond ids.';
CREATE INDEX audit_records_recorded_at_idx ON audit_records (recorded_at DESC);
```

- `action varchar(64) NOT NULL` is the short name of what was attempted; `detail varchar(500) NOT NULL` is free text; `recorded_at timestamptz NOT NULL` is the time, with time zone.
- `CREATE INDEX audit_records_recorded_at_idx ON audit_records (recorded_at DESC)` sorts newest first, because the audit endpoint always asks for the newest entries.

**The seed script, in full:**

```sql
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
```

- `INSERT INTO customers (name) VALUES ('Ada Example'), ('Grace Example');` adds two rows; the `id` column is filled by the identity counter (1 and 2).
- The `stock_items` insert adds three SKUs with their units on hand.
- The `orders_table` insert: `SELECT 1, ...` gives customer 1; `timestamptz '2026-10-27 00:00:00+00' + (n || ' hours')::interval` builds a timestamp by turning the text `n` followed by ` hours` into an interval (`||` joins text, `::interval` converts the type) and adding it to a fixed start; `generate_series(1, 20) AS n` produces 20 rows, numbered 1 to 20.
- The `order_lines` insert: for every order (`FROM orders_table o`) and each of three SKUs listed in `(VALUES (...), (...), (...)) AS s(sku)`, `CROSS JOIN` makes every combination: 20 orders times 3 SKUs is 60 lines. `1 + (o.id % 3)::int` is a quantity from 1 to 3 (`%` is the remainder of a division).
- The last insert adds one audit row.

**The queries, in full:**

```sql
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
```

- Query 1 lists customer 1's orders in id order: the single first statement of the naive listing.
- Query 2 reads the lines of one order: the statement that runs once **per order** when lines load lazily (20 orders mean 20 runs).
- Query 3 is the entity-graph statement: `LEFT JOIN` returns every order with each of its lines in one answer.
- Query 4 counts, per customer name, distinct orders (`count(DISTINCT o.id)`) and lines (`count(l.id)`); `GROUP BY c.name` makes one row per customer and `LEFT JOIN` keeps Grace, who has none.
- Query 5 finds orders without lines (`WHERE l.id IS NULL` keeps rows where the left join found no line). It must return no rows; a row would be the half-built order of the second trap.
- Query 6 shows stock and versions. Query 7 shows the newest audit entries (`ORDER BY id DESC LIMIT 10`).
- Query 8 reads PostgreSQL's own catalog table `pg_indexes` to list the indexes whose names end in `_idx`.

### The complete schema script

`sql/001_schema.sql` quoted whole. Lines starting with `--` are SQL comments: the database ignores them, and people read them. The statements are explained in the sections above.

### `sql/001_schema.sql`

The whole file, as it is in the repository:

```sql
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
```


## Try it yourself

1. `make up && make db && make queries` prints the result of every query in `sql/003_queries.sql`.
2. `make psql` then `explain select * from order_lines where order_id = 1;` shows whether the index is used.
3. Try to break a rule: `insert into order_lines (order_id, sku, quantity) values (1, 'x', 0);` fails the `CHECK`.

## Mistakes people make

- Building SQL by gluing user text into a string (SQL injection). JPA and JDBC use parameters.
- Forgetting an index on a foreign key column that queries filter by.
- Treating `NULL` as a value: `NULL = NULL` is not true; use `IS NULL`.
- Changing an applied migration file: see `flyway.md`.

## Check yourself

1. What is the difference between DDL and DML? *DDL defines structure; DML changes data.*
2. What does a foreign key guarantee? *The referenced row exists.*
3. Why is the table called `orders_table`? *`order` is a reserved word.*
4. What does an index do? *Lets the database find rows without scanning the table.*
5. What does `LEFT JOIN` keep? *Rows of the left table without a match.*

## Further reading

- PostgreSQL 18, "The SQL Language": https://www.postgresql.org/docs/18/sql.html
- Constraints: https://www.postgresql.org/docs/18/ddl-constraints.html
