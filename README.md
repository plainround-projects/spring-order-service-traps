# spring-order-service-traps

An order service on Spring Data JPA and PostgreSQL with three bugs that each pass code review and only show up under the right conditions: an N+1 listing, a `@Transactional` method called from its own class, and an audit write that rolls back with the checkout it records. Each bug is planted in test code, caught by a test that fails for the right reason, and fixed in production code.

| | |
| --- | --- |
| **Lesson** | `/prep/spring/project-order-service-traps`, "An order service with three planted traps" (`data-structure-explainer/src/content/tracks/spring/16-project-order-service-traps.md` in the Plainround app) |
| **Planned hours** | 10 |
| **Level** | Intermediate |
| **Versions** | Spring Boot 4.1.2 (Spring Framework 7.0, Hibernate 7), Java 25 (the current LTS; the lesson names none), PostgreSQL 18, Flyway 12, Maven 3.9 through the checked-in wrapper |
| **Architecture** | [ARCHITECTURE.md](ARCHITECTURE.md), decisions: [docs/adr](docs/adr), review checklist: [docs/REVIEW-CHECKLIST.md](docs/REVIEW-CHECKLIST.md) |
| **Technology notes** | [tech/README.md](tech/README.md): one note per technology used here, written for a complete beginner |
| **Author** | Sumit Srivastava |

## Prerequisites

| Tool | Version | Used for |
| --- | --- | --- |
| JDK | 25 (Temurin 25.0.2 tested; CI uses Temurin 25) | build and tests |
| Docker with Compose v2 | 24 or newer (tested with 24.0.6) | PostgreSQL for `make up`, Testcontainers for the integration tests |
| GNU Make, `curl`, `jq`, `openssl` | any recent | `make run`, `make env` |
| Images | `postgres:18` (pinned by digest in compose), `eclipse-temurin:25` (pinned by digest in the Dockerfile) | pulled on first use |

No cloud account. The database password is never in the repository: `make up` creates `.env` from `.env.example` and fills `POSTGRES_PASSWORD` with a random value. Ports 15432 (PostgreSQL) and 18102 (the service) can be changed in `.env`.

## Setup, run, test, stop

```bash
make up            # build the image, start PostgreSQL 18 and the service, wait until both are healthy
make run           # the checkout, order list, rollback and audit scenario over HTTP (scripts/scenario.sh)
make traps         # run the trap tests and print the numbers they measured
make test          # unit, slice and Testcontainers tests, then the coverage gate (needs Docker)
make lint          # Spotless format check + Checkstyle
make down          # stop the containers and delete the database volume
```

More targets (`make help` lists all): `make test-unit` (no Docker needed), `make db` and `make queries` (the SQL scripts in a scratch database), `make psql`, `make serve` (the service from source against the compose database), `make format`, `make audit`, `make docker-build`.

## Project structure

```text
spring-order-service-traps/
├── pom.xml, mvnw, .mvn/                  Maven build, wrapper checked in
├── src/main/java/dev/plainround/orders/
│   ├── domain/                           Customer, Order, OrderDraft, OrderLine, AuditEntry, named errors; ports: CustomerRepository, OrderRepository, StockRepository, AuditLog. Depends on nothing but the JDK.
│   ├── application/                      CheckoutService (the transaction boundary), OrderQueryService, CustomerService, StockService, AuditService. Depends on domain.
│   ├── infrastructure/                   ClockConfig
│   │   └── persistence/                  JPA entities (private to this package), Spring Data repositories, adapters: JpaOrderRepository, JpaStockRepository, JpaCustomerRepository, JpaAuditLog
│   └── interfaces/web/                   controllers, request/response records, OrdersExceptionHandler (problem details), RequestIdFilter. Nothing depends on it.
├── src/main/resources/
│   ├── application.properties            configuration from the environment, open-in-view=false, ddl-auto=validate
│   └── db/migration/                     Flyway V1 (customers, orders, lines), V2 (stock), V3 (audit)
├── src/test/java/dev/plainround/orders/  mirrored by package: domain/, application/, interfaces/web/
│   ├── traps/                            the three traps and the stretch work: NPlusOneIT, SelfInvocationIT, AuditIT, CheckoutIT, OpenInView*IT, OrderModelIT, SchemaMirrorIT, and the planted code (NaiveOrderJpaRepository, SelfInvokingCheckout, AuditTrapBeans)
│   ├── support/                          PostgresContainer (Testcontainers), DatabaseReset, Evidence
│   ├── ArchitectureTest.java             the dependency rule and the two proxy rules (ArchUnit)
│   ├── OrderServiceScenarioIT.java       the scenario over HTTP against a real server and database
│   └── ReviewChecklistTest.java, SqlMirrorTest.java, ApplicationConfigurationTest.java
├── sql/                                  001_schema.sql, 002_seed.sql, 003_queries.sql, migrations/ (mirror of Flyway)
├── docs/                                 REVIEW-CHECKLIST.md, adr/
├── tech/                                 one note per technology (Java, Spring, SQL, Docker, Bash, ...) for a complete beginner, with the repository\'s own lines explained
├── config/checkstyle/, scripts/          lint rules; scenario.sh, audit.sh
├── Dockerfile, docker-compose.yml, Makefile, .github/workflows/ci.yml, .env.example, .editorconfig, .gitignore
└── README.md, ARCHITECTURE.md, CHANGELOG.md, LICENSE
```

Dependency rule (domain depends on nothing; application depends on domain; infrastructure and interfaces depend on both; nothing depends on interfaces) is enforced by `ArchitectureTest`. The JPA entities may be used only inside `infrastructure.persistence`.

## The SQL

Every statement the project runs against PostgreSQL is a file under [sql/](sql/):

| File | Kind | What it holds | Run by |
| --- | --- | --- | --- |
| [001_schema.sql](sql/001_schema.sql) | DDL | all tables, keys, check constraints, indexes (with the reason for each), comments: the three migrations concatenated | `make db` |
| [002_seed.sql](sql/002_seed.sql) | DML | two customers, one with 20 orders of 3 lines (the size of the N+1 test), three SKUs of stock, one audit row | `make db` |
| [003_queries.sql](sql/003_queries.sql) | queries | the naive listing's two statements, the fetch-join statement, orders and lines per customer, the "order without a line" check for trap 2, stock, the audit trail, the indexes | `make queries` |
| [migrations/](sql/migrations/) | DDL, mirrored | `V1__customers_orders_lines.sql`, `V2__stock_items.sql`, `V3__audit_records.sql`: identical to `src/main/resources/db/migration`, which Flyway applies at startup | by the application; `SqlMirrorTest` fails if the copies differ |

`make db` creates the scratch database `orders_sql_demo` in the compose PostgreSQL and loads 001 and 002 into it, so it never touches the service's own database. `SchemaMirrorIT` proves `001_schema.sql` builds the same columns, indexes and constraints as the migrations. There are no explain files: this is not a tuning project, and the statement counts of the traps are measured by Hibernate statistics instead.

## Done when: the lesson's checklist and the proof for each line

The numbers below were printed by the integration tests (`target/evidence.txt`, also printed by `make traps`) against PostgreSQL 18 in Testcontainers.

| # | Done when (from the lesson) | Test | What it shows |
| --- | --- | --- | --- |
| 1 | A test saves a customer with two orders of three lines each and reads them back. | `OrderModelIT.savesAndReadsBackACustomerWithTwoOrdersOfThreeLinesEach` | passes on real PostgreSQL (`@DataJpaTest` with `replace = NONE` and `@ServiceConnection`), with `spring.jpa.open-in-view=false` set from the first commit and asserted by `OrderModelIT.openInViewIsExplicitlyOffInTheApplicationConfiguration` and `ApplicationConfigurationTest` |
| 2 | A test asserts the exact statement count for loading one order by id. | `OrderModelIT.loadingOneOrderByIdIsExactlyOneStatement` | `prepareStatementCount = 1` |
| 3 | The test passes with two queries or fewer for 20 orders, and still passes for 200. | `NPlusOneIT`: the naive listing (`theNaiveListingRunsOneQueryForTheOrdersAndOneMorePerOrderForItsLines`, `theNaiveCountKeepsGrowingWith200Orders`) and the fix (`fetchJoinLoadsTwentyOrdersWithTheirLinesInOneStatement`, `fetchJoinKeepsQueryCountFlatAt200Orders`) | naive: 21 statements for 20 orders, 201 for 200 (1 + N); fixed: 1 for 20 and 1 for 200. The lesson's example shows 22 for the naive case because its mapping layer touches one more lazy association; here the count is exactly 1 + N. |
| 4 | The self-invocation test first fails, then passes after the transactional method moves behind the proxy. | `SelfInvocationIT`: `selfInvocationLeavesAPartialOrderOnFailure`, `proxyFixedRollsBackCompletelyOnTheSameFailure`; on the real service `CheckoutIT.aLaterLineOutOfStockRollsBackTheOrderAndTheEarlierReservationButKeepsTheAudit` | orders after the failed self-invoked checkout: 1 (a half-built order); after the failed proxy-fixed checkout: 0. The test class is deliberately not `@Transactional`: a test-wide transaction would do the rolling back and hide the bug. |
| 5 | A test that forces checkout to fail finds the audit row, and a comment explains why the separate bean is required. | `AuditIT`: `sameTransactionAuditIsLostWhenCheckoutFails`, `auditWithRequiresNewSurvivesRollback`, `requiresNewOnTheSameBeanIsIgnored` | audit rows after a failed checkout: same transaction 0, `REQUIRES_NEW` on a separate bean 1, `REQUIRES_NEW` on the same bean 0 (annotation ignored). The explanation is the class comment on `JpaAuditLog`, and [ADR 0002](docs/adr/0002-audit-in-its-own-transaction.md). |
| 6 | Each of the five checklist lines points to the test in the project that would catch its bug. | `ReviewChecklistTest`, [docs/REVIEW-CHECKLIST.md](docs/REVIEW-CHECKLIST.md) | the test fails the build if a line names a test class or method that does not exist |

Written in the order the lesson asks (test first, see it fail, then fix): the planted version of each trap lives in test code (`NaiveOrderJpaRepository`, `SelfInvokingCheckout`, `AuditTrapBeans`), so the tests that assert the broken behavior stay in the suite as documentation of the failure, and production code only ever contains the fix.

## Stretch goals

| Stretch (from the lesson) | Built? |
| --- | --- |
| Optimistic locking with `@Version` and a test where two checkouts race for the last item. | **Yes.** `StockItemEntity` has `@Version`; `CheckoutIT.aTransactionThatReadAStaleStockVersionLosesTheRaceInsteadOfOverwriting` makes the race deterministic with a nested `REQUIRES_NEW` transaction (the loser gets `StockConflict`, HTTP 409 `concurrent-update`), and `racingCheckoutsForTheLastUnitsNeverOversell` runs eight real concurrent checkouts for three units and asserts nothing is oversold. |
| Replace the entity graph with a read-only projection query and compare the statements. | No. ARCHITECTURE.md, "How to extend it". |
| Turn `open-in-view` back on and show which lazy load it hides. | **Yes.** `OpenInViewIT` (off: the lazy load in the response step gives HTTP 500) and `OpenInViewOnIT` (on: HTTP 200, the bug hidden), using a test-only router-function endpoint that is never component-scanned. |

## What is simplified for teaching

- One database, no read replica; the order list is not paged (a fetch-joined collection cannot be paged in the database; ARCHITECTURE.md says what a real service does).
- No authentication; the stock endpoint is open.
- The service makes no outbound HTTP call, so the only outbound dependency is PostgreSQL: its calls are bounded by a connect timeout, a socket timeout, a pool wait timeout and a transaction timeout, and Hikari retries the first connection for up to 30 seconds at startup. A lost stock race is returned to the caller as 409, not retried by the service.
- Test database passwords are Testcontainers' throwaway defaults; the compose password is generated per machine.

## Operability

JSON logs with a `requestId` on every line; health with liveness and readiness probes (the database check is part of readiness); graceful shutdown (`server.shutdown=graceful`); a `Dockerfile` with a pinned JRE, user 10001 and a read-only root file system; the database password only from the environment (`POSTGRES_PASSWORD` has no default, so a missing value fails at startup instead of connecting with a guessable one).

## Verification status

Every row marked "pending capacity" needs Docker: restart Docker Desktop with free disk first (Docker.raw was at its 60 GiB cap on 2026-10-28), then run the command in that row.

Run on 2026-10-27 on a Mac that was very heavily loaded (load average above 100) and whose Docker became unusable during the night (see below), so some steps ran several times and some are pending.

| Check | Command | Result |
| --- | --- | --- |
| Unit and slice tests (no Docker) | `./mvnw test` (the unit phase of `verify`) | passed: 57 tests |
| Integration tests (Testcontainers, PostgreSQL 18) | `./mvnw verify` | 23 of 24 passed in the last full run, including the numbers above, the schema-mirror test and the HTTP scenario; the 24th, `CheckoutIT.racingCheckoutsForTheLastUnitsNeverOversell`, failed because the test pool of 4 connections was starved (each racing checkout holds one connection and its audit write needs a second). The test configuration now sets a pool of 20. **Pending capacity: run `make test`** to confirm that fix and the JaCoCo gates (domain at least 95 percent, whole project at least 85 percent), which were not evaluated because the build stopped at the failing test. |
| Lint | `make lint` (`./mvnw spotless:check checkstyle:check`) | passed: Spotless clean, 0 Checkstyle violations |
| Container: `make up`, `make run`, `make traps`, `make down` | pending capacity: run `make up && make run && make traps && make down` | not run: the Docker VM disk was full and read-only |
| SQL scripts | pending capacity: run `make up && make db && make queries` | not run, same reason; the schema script is proved by `SchemaMirrorIT`, which passed |
| Image build, pinned base images, non-root user | pending capacity: run `make docker-build` | not run, same reason |
| Dependency audit | pending capacity: run `make audit` | not run, same reason |
| Fresh-clone test | pending capacity: `git clone . /tmp/os && cd /tmp/os && make lint && make test && make up && make run && make down` | not run |

## License

MIT, Sumit Srivastava, 2026. See [LICENSE](LICENSE).
