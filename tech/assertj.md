# AssertJ

## What it is

AssertJ is a library of assertions: statements that check a result and throw a clear error when the result is wrong. You write `assertThat(actual)` and then chain what you expect: `.isEqualTo(5)`, `.contains("a")`, `.hasSize(3)`. The failure message shows both values.

## Why this repository uses it

Tests should read like sentences, and a failure should say what was expected and what happened. AssertJ also checks exceptions (`assertThatThrownBy`) without try/catch.

## Where it appears

Every test class imports `org.assertj.core.api.Assertions.assertThat`. It comes with `spring-boot-starter-test`.

## How it works underneath

1. `assertThat(x)` returns an object specialized for the type of `x` (a string assertion, a list assertion, a number assertion). That is why IDEs can suggest the right checks.
2. Each method such as `isEqualTo` compares and, if the check fails, throws `AssertionError` with a built message.
3. JUnit sees the thrown error and marks the test failed (see `junit-5.md`).
4. `as("text")` or a lambda `as(() -> body)` adds a description shown on failure.

## Walkthrough of this repository's use

```java
void theDraftKeepsItsOwnCopyOfTheLines() {
    List<OrderLine> lines = new ArrayList<>(List.of(new OrderLine("sku-1", 1)));
    OrderDraft draft = new OrderDraft(1, lines);

    lines.add(new OrderLine("sku-2", 1));

    assertThat(draft.lines()).hasSize(1);
}
```

`assertThat(draft.lines()).hasSize(1)` checks the list length.

```java
void theNaiveListingRunsOneQueryForTheOrdersAndOneMorePerOrderForItsLines() {
    long customerId = seedCustomerWithOrders(20);

    int lines =
            naive.findByCustomerId(customerId).stream()
                    .mapToInt(order -> order.getLines().size())
                    .sum();

    assertThat(lines).isEqualTo(60);
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + 20);
    Evidence.record("naive queries for 20 orders:  " + statistics.getPrepareStatementCount());
}
```

`assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + 20)` is the heart of the first trap: the number of SQL statements Hibernate prepared must be exactly one for the orders plus one for each of the 20 orders.

```java
void anUnknownCustomerLeavesOnlyTheAuditEntry() {
    stock.setAvailable("sku-1", 5);

    assertThatThrownBy(() -> checkout.checkout(999, List.of(new OrderLine("sku-1", 1))))
            .isInstanceOf(CustomerNotFound.class);

    assertThat(DatabaseReset.count(jdbc, "orders_table")).isZero();
    assertThat(DatabaseReset.count(jdbc, "audit_records")).isEqualTo(1);
}
```

`assertThatThrownBy(() -> checkout.checkout(...)).isInstanceOf(CustomerNotFound.class)` expects the named error.

## Try it yourself

1. Open a test and change an expected number, then run `./mvnw test -Dtest=<class>` and read the message: `expected: 6 but was: 5`.
2. Try `assertThat(List.of(1, 2)).containsExactly(2, 1)` in `jshell` with the AssertJ jar on the classpath, or in a scratch test, to see the order-sensitive message.
3. Search the tests for `.as(` to see failure descriptions.

## Mistakes people make

- `assertThat(a == b).isTrue()`: the failure says only "expected true". Use `assertThat(a).isEqualTo(b)`.
- Forgetting the last call: `assertThat(x)` alone checks nothing.
- Comparing `BigDecimal` with `isEqualTo`: `1.0` and `1.00` differ. This repository uses `isEqualByComparingTo` for money.

## Check yourself

1. What does `assertThat(x).isEqualTo(y)` do on failure? *Throws `AssertionError` showing both values.*
2. How do you expect an exception? *`assertThatThrownBy(() -> code).isInstanceOf(...)`.*
3. What does `extracting` do? *Maps each element of a collection to a field.*
4. What is `.as(...)` for? *A description shown when the assertion fails.*
5. Why `isEqualByComparingTo` for `BigDecimal`? *It ignores trailing zeros.*

## Further reading

- AssertJ Core documentation (version managed by Spring Boot 4.1.2, 3.27): https://assertj.github.io/doc/
