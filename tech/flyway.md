# Flyway (database migrations)

## What it is

A **migration** is a numbered SQL file that changes the database schema one step. **Flyway** is a tool that, at application startup, looks at which migrations the database has already run, and runs the new ones in version order. It records each run in a table called `flyway_schema_history`, with a checksum of the file.

## Why this repository uses it

The schema must have one owner. Flyway builds it from files in the repository; Hibernate only checks it (`ddl-auto=validate`). That makes the database reproducible: an empty database plus the application equals the right schema.

## Where it appears

| Path | Role |
| --- | --- |
| `src/main/resources/db/migration/V1__customers_orders_lines.sql`, `V2__stock_items.sql`, `V3__audit_records.sql` | the migrations |
| `sql/migrations/` | a byte-for-byte copy, checked by `SqlMirrorTest` |
| `pom.xml` | `spring-boot-starter-flyway` and `flyway-database-postgresql` |
| `application.properties` | nothing: Boot's defaults find the folder |

## How it works underneath

1. At startup, before Hibernate, Spring Boot creates a Flyway object and calls `migrate()`.
2. Flyway scans `classpath:db/migration` for names that match `V<version>__<description>.sql`. Two underscores separate the version from the description.
3. It reads `flyway_schema_history` (creating it on an empty database), compares, and runs each missing version in order, each in its own transaction.
4. If a migration that was already applied has changed (its checksum differs), Flyway refuses to start. Applied migrations are never edited; you add a new version.
5. The PostgreSQL support is in a separate module (`flyway-database-postgresql`) in Flyway 10 and later.

## Walkthrough of this repository's use

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

Version 3 adds the audit table. The comment above the table explains why rows are written in their own transaction. The index on `recorded_at DESC` serves "newest entries first".

`sql/001_schema.sql` is the three files concatenated for reading and for `make db`. `SchemaMirrorIT` proves it builds exactly the same columns, indexes and constraints as Flyway's run, by creating a second database from the script and comparing `information_schema` and `pg_catalog` descriptions of the two. `SqlMirrorTest` proves `sql/migrations/` equals the Flyway folder.

```java
void theSchemaScriptBuildsTheSameSchemaAsTheMigrations() throws Exception {
    List<String> fromMigrations;
    try (Connection connection = flywayDatabase.getConnection()) {
        fromMigrations = describe(connection);
        connection.createStatement().execute("CREATE DATABASE schema_script");
    }

    List<String> fromScript;
    try (Connection connection = connectTo("schema_script")) {
        connection.createStatement().execute(Files.readString(Path.of("sql/001_schema.sql")));
        fromScript = describe(connection);
    }

    assertThat(fromMigrations).isNotEmpty();
    assertThat(fromScript).isEqualTo(fromMigrations);
}
```

## Try it yourself

1. After `make up`: `make psql` then `select version, description, success from flyway_schema_history;` lists the three migrations.
2. Add a new file `V4__something.sql` in a scratch copy and restart: Flyway applies it.
3. Change a character in `V1` in a scratch copy and restart against the existing database: startup fails with a checksum mismatch.

## Mistakes people make

- Editing an applied migration instead of adding a new one.
- Wrong file name (one underscore, or a lower-case `v`): Flyway ignores it.
- Letting Hibernate create the schema in production (`ddl-auto=update`): the schema then has no history.
- Two developers using the same version number.

## Check yourself

1. What does `flyway_schema_history` record? *Which migrations ran, with checksums.*
2. What does Flyway do when an applied file changed? *Refuses to start.*
3. What do two underscores in `V1__name.sql` separate? *The version and the description.*
4. Who owns the schema, Flyway or Hibernate? *Flyway; Hibernate only validates.*
5. Why mirror the migrations in `sql/`? *The template wants every SQL file visible there; tests keep the copies equal.*

## Further reading

- Flyway documentation (12.x): https://documentation.red-gate.com/fd/flyway-documentation-138346877.html
- Spring Boot 4.1.2, database initialization (Flyway): https://docs.spring.io/spring-boot/4.1.2/how-to/data-initialization.html
