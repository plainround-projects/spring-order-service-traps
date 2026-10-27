# Bean Validation (Jakarta Validation)

## What it is

Bean Validation is a standard way to state rules about data with annotations: `@NotBlank` (text must have a visible character), `@Min(1)` (a number must be at least 1), `@Size(max = 200)`, `@Pattern(regexp = "...")`. A **validator** reads the annotations and reports every rule that is broken. The standard is called Jakarta Validation; the library that implements it here is Hibernate Validator (unrelated to the database Hibernate despite the name).

## Why this repository uses it

The lesson asks for "validation at the boundary": a request with a blank title must be rejected with a 400 that names the field, before any business code runs. The same idea guards every request body in this project.

## Where it appears

| Path | Rules |
| --- | --- |
| `interfaces/web/NewCustomerRequest.java`, `LineRequest.java`, `CheckoutRequest.java`, `StockRequest.java` | name, SKU, quantity and basket rules |
| `interfaces/web/OrderController.java`, `StockController.java`, `AuditController.java` | `@Valid` on bodies; constraints on a path variable and a query parameter |

## How it works underneath

1. A constraint annotation is only a label. Something has to run the validator. Spring MVC does it in two places: when a parameter is marked `@Valid` (the body is validated after it is read), and when a controller method parameter carries a constraint annotation directly (Spring validates the method call).
2. The validator inspects the object, collects **violations** (the field path, the message and the rule) and does not stop at the first.
3. If there are violations, Spring throws an exception (`MethodArgumentNotValidException` for a body, `HandlerMethodValidationException` for method parameters) before the controller method runs. The advice class turns it into a 400 answer.
4. The same validator can check a settings record at startup (`@Validated` on a `@ConfigurationProperties` class).

## Walkthrough of this repository's use

```java
public record CheckoutRequest(
        @NotEmpty(message = "lines must not be empty")
                @Size(max = OrderDraft.MAX_LINES, message = "lines must have at most 50 entries")
                List<@Valid LineRequest> lines) {

    /**
     * Converts to domain lines.
     *
     * @return the lines
     */
    public List<OrderLine> toDomain() {
        return lines.stream().map(LineRequest::toDomain).toList();
    }
}
```

`@NotEmpty` rejects `null` and an empty list; `@Size(max = ...)` caps it at 50 lines. `List<@Valid LineRequest>` puts `@Valid` on the **element type**: every line in the list is validated too, and the error path says which one (`lines[0].quantity`).

```java
public record LineRequest(
        @NotBlank(message = "sku must not be blank")
                @Size(max = OrderLine.MAX_SKU_LENGTH, message = "sku must be at most 64 characters")
                String sku,
        @Min(value = 1, message = "quantity must be at least 1")
                @Max(value = OrderLine.MAX_QUANTITY, message = "quantity must be at most 1000")
                int quantity) {
```

A SKU must not be blank and is at most 64 characters; the quantity is from 1 to 1000. The constants come from the domain's `OrderLine`, so the web rule and the domain rule cannot drift apart.

The rule a field annotation cannot express, a SKU appearing twice, lives in the domain (`OrderDraft`) and becomes a 422 answer.

```java
public StockResponse set(
        @PathVariable @NotBlank @Size(max = 64, message = "sku must be at most 64 characters")
                String sku,
        @Valid @RequestBody StockRequest request) {
    service.setAvailable(sku, request.available());
    return new StockResponse(sku, request.available());
}
```

Here a constraint (`@NotBlank @Size(max = 64)`) sits directly on the path variable, and `@Valid` sits on the body. When any parameter has a constraint, Spring validates the whole method call and reports problems as a `HandlerMethodValidationException`; the advice class reads both kinds of exception.

## Try it yourself

1. `curl -s -X POST localhost:18102/customers/1/orders -H 'Content-Type: application/json' -d '{"lines":[{"sku":"","quantity":0}]}'` lists `lines[0].sku` and `lines[0].quantity`.
2. `./mvnw test -Dtest=OrderControllerTest` shows the same checks in the web-layer tests.

## Mistakes people make

- Putting the annotations on the domain object instead of a request type: it couples what the API accepts to what it stores.
- Forgetting `@Valid` on the parameter, or on the element type of a list.
- Using `@NotNull` for text: an empty string passes. Use `@NotBlank`.
- Trusting the web rules alone: the domain records re-check their own rules, so a bug in the web layer cannot create a broken object.

## Check yourself

1. Which annotation rejects `"   "`? *`@NotBlank`.*
2. What does `@Valid` on a parameter do? *Makes Spring run the validator on it.*
3. How is a violation in the 7th basket line reported? *With a path such as `lines[6].quantity`.*
4. Why is the request a different type from the domain object? *A client must not be able to set fields like `id`.*
5. What does `@Validated` on a settings record do? *Validates it when it is built at startup.*

## Further reading

- Jakarta Validation 3.1 specification: https://jakarta.ee/specifications/bean-validation/3.1/
- Spring Framework 7.0.10, "Validation": https://docs.spring.io/spring-framework/reference/7.0.10/core/validation/beanvalidation.html
- Hibernate Validator constraints: https://docs.jboss.org/hibernate/validator/9.0/reference/en-US/html_single/
