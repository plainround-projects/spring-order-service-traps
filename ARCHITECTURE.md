# Architecture: spring-order-service-traps

An order service on Spring Data JPA and PostgreSQL: customers, orders with lines, a checkout, a list of a customer's orders with their lines, and an audit trail. The service is the vehicle; the project is the three JPA traps that pass code review (an N+1 listing, a transactional method called from its own class, an audit write that rolls back with the checkout it records), each planted in test code, caught by a test and fixed in production code.

## Components and what each owns

These are the lesson's diagram boxes. The lesson draws everything inside one `OrderService` class ("list, checkout, save"); here the same responsibilities sit in separate beans, because two of the traps are about the boundary between beans.

| Lesson diagram | Class | Layer | Owns |
| --- | --- | --- | --- |
| Orders API (client) | `OrderController`, `CustomerController`, `StockController`, `AuditController`, `OrdersExceptionHandler` | interfaces | HTTP, request validation, problem documents; no logic |
| List orders (service) | `OrderQueryService.listOrders` | application | a read-only transaction around "check the customer, load the orders"; the fix for trap 1 lives in the query it calls |
| checkout() (service) | `CheckoutService.checkout` | application | the transaction boundary: `@Transactional` on the public entry point (the fix for trap 2) |
| saveOrder() (service) | `OrderRepository.place` implemented by `JpaOrderRepository.place` | domain port, infrastructure adapter | stores the order and its lines in the caller's transaction |
| Audit writer (service) | `AuditLog` implemented by `JpaAuditLog` | domain port, infrastructure adapter | its own bean with `REQUIRES_NEW`, so it commits on its own (the fix for trap 3) |
| PostgreSQL (db) | PostgreSQL 18, Flyway migrations `V1` to `V3` | outside | the schema (`orders_table`, `order_lines`, `customers`, `stock_items`, `audit_records`) |
| (support) | `StockRepository` implemented by `JpaStockRepository` | domain port, infrastructure adapter | units on hand, guarded by an optimistic-lock version (the lesson's stretch) |
| (support) | `Customer`, `Order`, `OrderDraft`, `OrderLine`, `AuditEntry`, named errors | domain | invariants and vocabulary; depends on nothing but the JDK |

JPA entities (`CustomerEntity`, `OrderEntity`, `OrderLineEntity`, `StockItemEntity`, `AuditRecordEntity`) live in `infrastructure.persistence` and never leave it: adapters map them to the domain records inside the transaction, and `ArchitectureTest.persistenceEntitiesStayInsideTheirPackage` fails the build if anything else touches one.

## Data flow

```mermaid
flowchart LR
    api([Orders API client])
    subgraph web["interfaces.web"]
        ctrl[OrderController]
        advice[OrdersExceptionHandler]
    end
    subgraph app["application"]
        list["OrderQueryService.listOrders<br/>(readOnly transaction)"]
        checkout["CheckoutService.checkout<br/>(@Transactional entry point)"]
    end
    subgraph infra["infrastructure.persistence"]
        orders[JpaOrderRepository<br/>fetch join: 1 statement]
        stock[JpaStockRepository<br/>@Version]
        audit["JpaAuditLog<br/>REQUIRES_NEW, own bean"]
    end
    db[(PostgreSQL 18)]

    api -->|GET orders| ctrl --> list --> orders -->|1 join query| db
    api -->|POST orders| ctrl --> checkout
    checkout -->|1. CHECKOUT_ATTEMPT| audit -->|own commit| db
    checkout -->|2. place order + lines| orders -->|same transaction| db
    checkout -->|3. reserve each line| stock -->|same transaction| db
    ctrl -. named error .-> advice -->|problem+json| api
```

### One checkout, step by step

1. `OrderController.checkout` validates the body (`@Valid`: 1 to 50 lines, SKU, quantity 1 to 1000) and calls `CheckoutService.checkout` through its Spring proxy, which opens one transaction.
2. `AuditLog.record("CHECKOUT_ATTEMPT", ...)` is called on another bean, so its proxy suspends the checkout transaction and commits the audit row in a new one. From now on the attempt is on record whatever happens.
3. `CustomerService.require` joins the transaction and fails with `CustomerNotFound` (404) for an unknown customer.
4. `new OrderDraft(...)` re-checks the rules a field annotation cannot express (distinct SKUs); `OrderRepository.place` stores the order and its lines with one cascade.
5. `StockRepository.reserve` runs for every line: it loads the stock row, refuses with `OutOfStock` (409) if there are too few units, otherwise decrements and flushes. The row carries a version; a concurrent checkout that changed it first makes the flush fail and the adapter raises `StockConflict` (409, safe to retry).
6. On any exception the proxy rolls the transaction back: the order, its lines and the earlier reservations vanish together; the audit row from step 2 stays.
7. On success the transaction commits and the controller answers 201 with the stored order.

## Decisions and trade-offs

| Decision | Why | Cost |
| --- | --- | --- |
| Ports and adapters with a pure domain; JPA entities private to `infrastructure.persistence` ([ADR 0001](docs/adr/0001-entities-stay-in-infrastructure.md)) | The lesson's traps are about Spring and Hibernate behavior, so the code that shows them is the infrastructure and the transaction boundary; keeping entities out of the domain keeps the rest testable without a database | a mapping function per aggregate; accepted, it is also where the lazy-load decisions are visible |
| Transaction on the application service's public method, never on a helper it calls | A `@Transactional` method called from its own class is skipped by the proxy (trap 2); `ArchitectureTest.noSelfInvocationOfTransactionalMethods` makes the pattern a build error | none |
| Audit through a port whose adapter is a separate bean with `REQUIRES_NEW` ([ADR 0002](docs/adr/0002-audit-in-its-own-transaction.md)) | An audit row written in the checkout transaction vanishes with it (trap 3); `REQUIRES_NEW` on the same bean is ignored for the same proxy reason as trap 2 | one extra connection for the instant of the audit write |
| Entity graph on the listing query (`@EntityGraph(attributePaths = "lines")`) | One join statement for any number of orders (trap 1) | a join returns each order once per line; Hibernate collapses it, but paging in the database over a fetched collection would not be possible, so the list is not paged (see limits) |
| `spring.jpa.open-in-view=false` and `ddl-auto=validate` from the first commit | A lazy load outside a transaction fails loudly; Hibernate checks the schema Flyway built instead of changing it | a request that needs lazy data must load it inside a service method |
| Optimistic locking (`@Version`) on stock instead of a conditional `UPDATE` | The lesson's stretch asks for it; the conflict becomes a named error with a retry meaning | a lost race costs the whole checkout and a retry by the caller |
| Flyway migrations mirrored in `sql/`; `sql/001_schema.sql` proved equal by `SchemaMirrorIT` | The migrations are the truth; the single script is for reading and for `make db` | one more file to keep in step; the test fails when they differ |
| Java 25 (current LTS), Spring Boot 4.1, PostgreSQL 18 | The lesson names no Java version; Boot 4 is the generation the Spring track teaches (Framework 7, Hibernate 7) | none |

## Failure modes the lesson teaches, and where the code shows them

| Trap | What a reviewer sees | What happens | Test that catches it | The fix in production code |
| --- | --- | --- | --- | --- |
| 1. N+1 | a loop over `order.getLines()` | 1 query for the orders plus 1 per order: 21 statements for 20 orders, 201 for 200 | `NPlusOneIT.theNaiveListingRunsOneQueryForTheOrdersAndOneMorePerOrderForItsLines`, `theNaiveCountKeepsGrowingWith200Orders` | `OrderJpaRepository.findByCustomerIdWithLines`; `NPlusOneIT.fetchJoinKeepsQueryCountFlatAt200Orders` asserts at most 2 statements at 20 and at 200 |
| 2. Self-invocation | `checkout()` calls `this.saveOrder()`, which has `@Transactional` | no transaction exists; a failure after the first line leaves a half-built order (1 order, 1 line) | `SelfInvocationIT.selfInvocationLeavesAPartialOrderOnFailure` (the class is not `@Transactional` on purpose) | `@Transactional` on `CheckoutService.checkout`; `SelfInvocationIT.proxyFixedRollsBackCompletelyOnTheSameFailure` (0 orders), `CheckoutIT.aLaterLineOutOfStockRollsBackTheOrderAndTheEarlierReservationButKeepsTheAudit` on the real service |
| 3. Lost audit | the audit save sits inside the checkout method | the audit row rolls back with the failed checkout | `AuditIT.sameTransactionAuditIsLostWhenCheckoutFails` | `JpaAuditLog` as its own bean; `AuditIT.auditWithRequiresNewSurvivesRollback`; `AuditIT.requiresNewOnTheSameBeanIsIgnored` shows why the separate bean is required |
| Hidden lazy load | the controller serializes an entity | with open-in-view on, the lazy load works by accident; off, it fails | `OpenInViewIT.aLazyLoadOutsideATransactionFailsWhenOpenInViewIsOff` (500), `OpenInViewOnIT` (200, the bug hidden) | `open-in-view=false` in `application.properties`, asserted by `ApplicationConfigurationTest` |
| Lost update on the last unit | two checkouts read the same stock row | without a version the second overwrites the first and oversells | `CheckoutIT.aTransactionThatReadAStaleStockVersionLosesTheRaceInsteadOfOverwriting`, `racingCheckoutsForTheLastUnitsNeverOversell` | `@Version` on `StockItemEntity`, `StockConflict` |

`ArchitectureTest` also forbids `@Transactional` on private methods (never proxied) and a call from a class to a `@Transactional` method of the same class, so traps 2 and 3 cannot return unnoticed.

## Limits of the teaching build

- The order list is not paged. A fetch-joined collection cannot be paged in the database (Hibernate would page in memory), so a real service pages the orders first and loads their lines with a second query; this is the first thing to change.
- Authentication, authorization and money are out of scope; SKUs are free text and stock is set through an open endpoint.
- One database, no read replica, no connection-pool tuning beyond bounded timeouts.
- A lost stock race is reported to the caller (409 `concurrent-update`) and is not retried by the service.
- Database calls are bounded (connect, socket, pool and transaction timeouts); there is no outbound HTTP, so no retry-with-backoff policy applies beyond Hikari waiting for the database at startup.

## How to extend it

- Replace the entity graph with a read-only projection (the lesson's second stretch): add a DTO query to `OrderJpaRepository`, compare the statements with `NPlusOneIT` style assertions.
- Page the list: query `select o.id ... limit` first, then `where o.id in (...)` with the entity graph.
- Add a real payment step to `CheckoutService` behind a port; give its adapter timeouts and a bounded retry, as the `spring-resilient-client` project does.
