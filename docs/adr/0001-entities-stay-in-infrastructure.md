# 0001. JPA entities stay in infrastructure

Date: 2026-10-27. Status: accepted.

## Context

The lesson maps `Customer`, `Order` and `OrderLine` as JPA entities with lazy relationships and puts all logic in one `OrderService`. The project's rule set asks for a domain that depends on nothing but the JDK and for a dependency rule that a test enforces.

## Decision

Entities are `*Entity` classes in `infrastructure.persistence`. The domain has plain records (`Customer`, `Order`, `OrderLine`) and ports (`CustomerRepository`, `OrderRepository`, `StockRepository`, `AuditLog`). Adapters map between the two inside the caller's transaction. An ArchUnit rule forbids any other package from depending on an entity.

## Alternatives considered

- Entities as the domain model (the lesson's shape): fewer classes, but the domain then depends on `jakarta.persistence`, and a lazy collection can leak into the web layer, which is exactly trap 1's cousin.
- Spring Data JDBC (no lazy loading at all): removes the traps instead of teaching them.

## Consequences

- The traps are reproduced in test code that uses the entities directly (`NaiveOrderJpaRepository`, `SelfInvokingCheckout`), while production code shows the fixes.
- Every aggregate needs a mapping function; the lazy-loading decision is visible there.
