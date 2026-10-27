# Spring transactions (@Transactional, proxies, REQUIRES_NEW)

## What it is

A **transaction** groups database changes so that all of them commit or none do. In Spring you mark a method `@Transactional`; Spring opens a transaction before the method, commits when it returns normally, and **rolls back** when it throws an unchecked exception. The **propagation** setting says what to do when a transaction already exists: `REQUIRED` (the default) joins it; `REQUIRES_NEW` suspends it and starts a separate one.

## Why this repository uses it

The second and third traps are about this annotation: a `@Transactional` method called from its own class is silently not transactional, and an audit record written inside the checkout transaction vanishes when the checkout fails.

## Where it appears

| Path | Role |
| --- | --- |
| `application/CheckoutService.java` | `@Transactional` on the public `checkout` method |
| `application/OrderQueryService.java`, `CustomerService.java`, `StockService.java` | `@Transactional(readOnly = true)` on reads |
| `infrastructure/persistence/JpaAuditLog.java` | `@Transactional(propagation = REQUIRES_NEW)` on a separate bean |
| `src/test/java/.../traps/SelfInvokingCheckout.java`, `AuditTrapBeans.java` | the bugs, planted |
| `ArchitectureTest.java` | two rules that make the traps fail the build |

## How it works underneath

1. Spring wraps a bean that has `@Transactional` methods in a **proxy** (see `spring-framework.md`). A call from outside goes proxy → open transaction → real method → commit or roll back.
2. The transaction is tied to the current **thread**: Spring keeps the open connection in a thread-local holder, and every repository call on that thread uses it.
3. `readOnly = true` is a hint: it lets Hibernate skip dirty checking and lets the driver and database optimize.
4. With `REQUIRES_NEW`, Spring suspends the current transaction (the first connection waits), takes a second connection, runs the method in a new transaction, commits it, and resumes the first. A failure later in the outer transaction does not undo it.
5. **Self-invocation:** inside the real object, `this.other()` calls the method directly, not through the proxy. The annotation on `other` is never seen.
6. `@Transactional` on a `private` method does nothing: a proxy cannot override it.

## Walkthrough of this repository's use

**The fix for trap 2: the transaction starts on the public entry point.**

```java
@Transactional
public Order checkout(long customerId, List<OrderLine> lines) {
    audit.record(
            "CHECKOUT_ATTEMPT", "customer " + customerId + ", " + lines.size() + " line(s)");
    customers.require(customerId);
    Order placed = orders.place(new OrderDraft(customerId, lines), clock.instant());
    lines.forEach(line -> stock.reserve(line.sku(), line.quantity()));
    LOG.info("order placed id={} customerId={}", placed.id(), customerId);
    return placed;
}
```

Everything inside `checkout` runs in one transaction: the audit call (which has its own transaction, see below), the customer check, storing the order, and every stock reservation. If `stock.reserve` throws `OutOfStock` for the second line, the exception passes through the proxy, which rolls back: the order, its lines and the first reservation vanish together.

**The bug, planted in test code.**

```java
public void checkoutSelfInvocationBug(Long customerId, boolean failHalfway) {
    this.saveOrder(customerId, failHalfway);
}
```

`checkoutSelfInvocationBug` has no annotation and calls `this.saveOrder(...)`. `saveOrder` is `@Transactional`, but the call does not pass through the proxy, so no transaction exists. Each `orders.save(...)` then commits on its own (Spring Data's `save` is transactional by itself), so when the failure comes after the first line is saved, a half-built order stays in the database. `SelfInvocationIT.selfInvocationLeavesAPartialOrderOnFailure` asserts exactly that: 1 order, 1 line. `proxyFixedRollsBackCompletelyOnTheSameFailure` runs the same failure through `checkoutProxyFixed`, whose entry point is annotated, and finds 0.

**The fix for trap 3: a separate bean with REQUIRES_NEW.**

```java
@Component
public class JpaAuditLog implements AuditLog {

    private final AuditJpaRepository audits;
    private final Clock clock;

    /**
     * Wires the adapter.
     *
     * @param audits the Spring Data repository
     * @param clock the time source
     */
    public JpaAuditLog(AuditJpaRepository audits, Clock clock) {
        this.audits = audits;
        this.clock = clock;
    }
```

The class comment explains why this must be its own bean. `CheckoutService` calls `audit.record(...)` on another bean, so the call crosses a proxy and `REQUIRES_NEW` applies: the audit row commits at once. `AuditIT` has three tests: the same-transaction audit is lost on failure (0 rows), the `REQUIRES_NEW` audit on a separate bean survives (1 row), and `REQUIRES_NEW` on a method of the **same** bean is ignored (0 rows), the false fix.

**Rules in the architecture test:**

```java
static final ArchRule noSelfInvocationOfTransactionalMethods =
        noClasses().should(callATransactionalMethodOfTheirOwnClass());

/** A private method is never proxied, so a {@code @Transactional} on it is silently ignored. */
@ArchTest
static final ArchRule transactionalIsNeverOnAPrivateMethod =
```

A custom condition looks at every call a class makes and fails the build if the target is a `@Transactional` method of the same class; a second rule forbids `@Transactional` on private methods. A mistake that passes code review becomes a red build.

**Concurrency with connections.** `REQUIRES_NEW` takes a second connection while the first is held. With a pool of 10, ten checkouts at the same time could each hold one connection and wait for a second: a deadlock-like stall. The test configuration uses a pool of 20 for the eight-thread race test (ADR 0002).

## Try it yourself

1. `make traps` prints "orders after failed self-invocation checkout: 1" and "...proxy-fixed checkout: 0", and the audit rows for the three variants (needs Docker).
2. In a scratch copy, move `@Transactional` from `checkout` to a private helper it calls, and run `ArchitectureTest`: a rule names the problem.
3. `./mvnw test -Dtest=ArchitectureTest` runs the rules without Docker.

## Mistakes people make

- Testing a transactional bug with a `@Transactional` test: the test's own transaction does the rolling back and hides the bug.
- Fixing self-invocation by annotating the caller too, leaving the anti-pattern in place.
- Catching the exception inside the transactional method: Spring never sees it, so it commits.
- Checked exceptions: by default they do **not** roll back.

## Check yourself

1. What triggers a rollback by default? *An unchecked exception leaving the method.*
2. Why does `this.saveOrder()` skip the transaction? *The call bypasses the proxy.*
3. What does `REQUIRES_NEW` do? *Suspends the current transaction and starts a separate one.*
4. Why must the audit writer be a separate bean? *The annotation works only through the proxy.*
5. Why can `REQUIRES_NEW` stall a small pool? *It needs a second connection while holding the first.*

## Further reading

- Spring Framework 7.0.10, "Transaction Management": https://docs.spring.io/spring-framework/reference/7.0.10/data-access/transaction.html
- Declarative transactions and propagation: https://docs.spring.io/spring-framework/reference/7.0.10/data-access/transaction/declarative/tx-propagation.html
