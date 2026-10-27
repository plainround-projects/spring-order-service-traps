# PostgreSQL 18

## What it is

PostgreSQL ("Postgres") is a **relational database server**: a program that stores data in **tables** (rows and columns) and answers questions written in SQL. It runs as its own process and listens on a network port (5432 by default). Your application connects to it, sends SQL, and gets rows back. Postgres guarantees that a group of changes made in a **transaction** either all happen or none do.

## Why this repository uses it

The lesson is about JPA traps that only show on a real database: statement counts, transaction boundaries, rollbacks, locking. The lesson insists on PostgreSQL rather than an in-memory stand-in, because dialect and locking behavior differ.

## Where it appears

| Path | Role |
| --- | --- |
| `docker-compose.yml` | the `postgres` service, image `postgres:18` pinned by digest |
| `src/main/resources/db/migration/*.sql` | the schema (see `flyway.md`, `sql.md`) |
| `src/test/java/.../support/PostgresContainer.java` | starts a throwaway PostgreSQL 18 for the integration tests (see `testcontainers.md`) |
| `application.properties` | the JDBC URL, user and password |
| `Makefile` (`make psql`, `make db`) | open a SQL prompt; load the SQL scripts into a scratch database |

## How it works underneath

1. The `postgres` process owns the data files. Many client connections talk to it; each connection gets its own server process.
2. The application reaches it through **JDBC**, Java's standard database interface, with a driver (`org.postgresql:postgresql`). The **URL** `jdbc:postgresql://host:port/database` says where.
3. Opening a connection is slow, so a **connection pool** (HikariCP here) keeps a few open and lends them out.
4. A **transaction** starts with `BEGIN` and ends with `COMMIT` or `ROLLBACK`. Rows changed inside are invisible to others until commit. At the default isolation level (READ COMMITTED) each statement sees data committed before it started.
5. Constraints (primary keys, foreign keys, `CHECK`) are enforced by the database whatever the application does. This is the last line of defense.
6. Identity columns (`GENERATED ALWAYS AS IDENTITY`) hand out ids from a counter that never rolls back.

## Walkthrough of this repository's use

```yaml
postgres:
  image: postgres:18@sha256:edb0c08dc33befd0262ab3b5cf1c4f3f8cfd8b09e88c05b45adee29d6761ddb6
  environment:
    POSTGRES_USER: ${POSTGRES_USER:-orders}
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?run make env, or set POSTGRES_PASSWORD in .env}
    POSTGRES_DB: ${POSTGRES_DB:-orders}
  ports:
    - "${POSTGRES_PORT:-15432}:5432"
  volumes:
    - pgdata:/var/lib/postgresql
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
    interval: 3s
    timeout: 3s
    retries: 20

order-service:
```

- `image: postgres:18@sha256:...` pins the exact image.
- `environment` sets the first user, password and database name when the data directory is created. `${POSTGRES_PASSWORD:?run make env, or set POSTGRES_PASSWORD in .env}` stops compose with that message if the password is missing.
- `ports: "${POSTGRES_PORT:-15432}:5432"` publishes the database on host port 15432, so tools on your machine can connect.
- `volumes: pgdata:/var/lib/postgresql` keeps the data in a named volume between restarts; `make down` removes it.
- `healthcheck` runs `pg_isready`, which succeeds when the server accepts connections. The service waits for it.

The connection settings:

```properties
spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:15432}/${POSTGRES_DB:orders}?connectTimeout=3&socketTimeout=30&tcpKeepAlive=true
spring.datasource.username=${POSTGRES_USER:orders}
spring.datasource.password=${POSTGRES_PASSWORD}
```

The URL has placeholders with defaults. After the `?` are driver options: `connectTimeout=3` seconds to open a connection, `socketTimeout=30` seconds of silence before giving up on a query, `tcpKeepAlive=true`. The password has no default on purpose.

## Try it yourself

1. After `make up`: `make psql` opens a SQL prompt on the service's database.
2. Inside it: `\dt` lists tables; `\d orders_table` describes one; `select count(*) from audit_records;`; `\q` quits.
3. `make db && make queries` loads the schema and seed data into a scratch database and runs the lesson's queries.
4. `docker compose exec postgres pg_isready -U orders` prints `accepting connections`.

## Mistakes people make

- Connecting to `localhost` from inside another container: use the service name `postgres`.
- Opening a connection per request: use the pool.
- Trusting the application alone for rules: the `CHECK` and `UNIQUE` constraints are in the schema too.
- Forgetting the data volume survives `docker compose down` unless `-v` is given.

## Check yourself

1. What is a transaction? *A group of changes that all commit or all roll back.*
2. Why a connection pool? *Opening connections is slow.*
3. What does `socketTimeout=30` do? *Gives up on a query that gets no answer for 30 seconds.*
4. What does `pg_isready` check? *That the server accepts connections.*
5. Why does the database also have constraints? *They protect the data whatever the application does.*

## Further reading

- PostgreSQL 18 documentation: https://www.postgresql.org/docs/18/index.html
- PostgreSQL JDBC driver (42.7.x): https://jdbc.postgresql.org/documentation/
