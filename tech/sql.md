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
