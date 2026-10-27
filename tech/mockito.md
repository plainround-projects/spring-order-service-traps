# Mockito

## What it is

A **mock** is a fake object that records how it was called and returns answers you tell it to. Mockito is the library that creates mocks. You write `when(repository.findById("x")).thenReturn(...)` to script an answer and `verify(repository).save(book)` to check a call happened.

## Why this repository uses it

A unit test of one class should not need a database, a network or the rest of the application. Because each class receives its collaborators through its constructor, a test can pass mocks. `CheckoutServiceTest` checks that a checkout calls the audit log first, then reserves stock line by line, using mocks of all its collaborators.

## Where it appears

| Path | Role |
| --- | --- |
| `application/CheckoutServiceTest.java`, `QueryAndCustomerServicesTest.java` | `mock(...)` of the ports |
| `interfaces/web/*ControllerTest.java` | `@MockitoBean` for the services behind a controller |

## How it works underneath

1. `mock(Type.class)` makes, at run time, a subclass or proxy of the type whose every method does nothing and returns a default (`null`, 0, an empty collection).
2. `when(call).thenReturn(value)` records what the next identical call should answer. `thenThrow(...)` makes it throw.
3. `verify(mock).method(args)` fails the test if that exact call did not happen. `inOrder(...)` also checks the order.
4. In a Spring test, `@MockitoBean` puts a mock into the application context **in place of** the real bean of that type.
5. Mocking classes (not just interfaces) needs a Java agent; the build passes `-XX:+EnableDynamicAgentLoading` so newer JDKs stay quiet.

## Walkthrough of this repository's use

```java
void checkoutAuditsFirstThenPlacesTheOrderThenReservesEveryLine() {
    Order placed = service.checkout(1, LINES);

    assertThat(placed.id()).isEqualTo(10);
    InOrder inOrder = inOrder(audit, customers, orders, stock);
    inOrder.verify(audit).record("CHECKOUT_ATTEMPT", "customer 1, 2 line(s)");
    inOrder.verify(customers).require(1);
    inOrder.verify(orders).place(new OrderDraft(1, LINES), NOW);
    inOrder.verify(stock).reserve("sku-1", 2);
    inOrder.verify(stock).reserve("sku-2", 1);
}
```

`inOrder(audit, customers, orders, stock)` then `inOrder.verify(...)` lines check the sequence: audit first, then the customer check, then storing the order, then one reservation per line. That order is a business rule, so it is tested.

```java
void anOutOfStockLineStopsTheCheckoutAndLaterLinesAreNotReserved() {
    doThrow(new OutOfStock("sku-1")).when(stock).reserve("sku-1", 2);

    assertThatThrownBy(() -> service.checkout(1, LINES)).isInstanceOf(OutOfStock.class);

    verify(stock, never()).reserve("sku-2", 1);
}
```

`doThrow(new OutOfStock("sku-1")).when(stock).reserve("sku-1", 2)` scripts a failure; `verify(stock, never()).reserve("sku-2", 1)` checks the second line was not tried.

```java
@MockitoBean private CheckoutService checkout;

@MockitoBean private OrderQueryService queries;
```

`@MockitoBean` replaces the real service in the web-layer test, so only the controller and the advice run.

## Try it yourself

1. `./mvnw test -Dtest=CheckoutServiceTest`.
2. In a copy of a test, delete a `when(...)` line: the mock returns `null` or empty and the test fails in an informative way.
3. Change a `verify` to expect a different argument and read the "Wanted but not invoked" message.

## Mistakes people make

- Mocking everything, including simple value objects: use the real record.
- Verifying private details instead of behavior.
- Forgetting that an unscripted mock returns `null` / 0 / empty.
- Mocking the class under test.

## Check yourself

1. What does an unscripted mock method return? *A default: `null`, 0, empty.*
2. How do you check call order? *`inOrder(...)`.*
3. What does `@MockitoBean` do? *Replaces a bean in the Spring test context with a mock.*
4. When should you use the real object? *For simple values and when the real one is cheap and deterministic.*
5. Why can a lambda stand in for `IdGenerator`? *It has one method.*

## Further reading

- Mockito documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
- Spring Framework 7.0.10, bean overrides (`@MockitoBean`): https://docs.spring.io/spring-framework/reference/7.0.10/testing/annotations/integration-spring/annotation-mockitobean.html
