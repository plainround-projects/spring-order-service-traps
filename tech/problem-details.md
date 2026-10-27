# Problem Details (RFC 9457, application/problem+json)

## What it is

When an HTTP request fails, the server should say why in a shape that programs can read. **Problem Details** is a standard (RFC 9457) for that shape: a JSON object with a few agreed fields, sent with the content type `application/problem+json`. The fields: `type` (a URI that names the kind of problem), `title` (a short human summary), `status` (the HTTP status code), `detail` (an explanation of this occurrence) and `instance` (the URL that failed). A service may add its own fields; this repository adds `errors` for validation.

## Why this repository uses it

The lesson asks for "one error shape" for the project's errors and the framework's own: an unknown id, a malformed JSON body, a failed validation. A client that parses errors then needs one parser. Spring has the type `ProblemDetail` for it.

## Where it appears

| Path | What it does |
| --- | --- |
| `interfaces/web/OrdersExceptionHandler.java` | builds a `ProblemDetail` for each named error |
| `application.properties` | `spring.mvc.problemdetails.enabled=true`, the lesson's switch for the framework's own errors |

## How it works underneath

1. A controller or the framework throws an exception.
2. An `@ExceptionHandler` method in the advice class returns a `ProblemDetail` object (or a `ResponseEntity` that holds one).
3. Spring converts it to JSON with the content type `application/problem+json`. Fields you add with `setProperty(name, value)` are written at the top level of the JSON.
4. The `instance` field is filled by Spring from the request URL when you leave it empty.
5. For the framework's own exceptions (a wrong HTTP method, a body that is not JSON) the base class `ResponseEntityExceptionHandler` already knows how to build a `ProblemDetail`. The advice here extends that base class so those errors share the shape.

## Walkthrough of this repository's use

```java
private static ProblemDetail problem(
        HttpStatus status, String name, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create(PROBLEM_URN + name));
    problem.setTitle(title);
    return problem;
}
```

`ProblemDetail.forStatusAndDetail` creates the object; `setType(URI.create("urn:plainround:problem:" + name))` names the kind (`out-of-stock`, `customer-not-found`, `concurrent-update`, ...); `setTitle` is the short text.

```java
@ExceptionHandler({InvalidOrder.class, InvalidCustomer.class})
public ProblemDetail handleInvalid(RuntimeException exception) {
    return problem(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "invalid-order",
            "Invalid request",
            exception.getMessage());
}
```

A rule the field annotations cannot express (the same SKU twice in one basket) is an `InvalidOrder` from the domain, answered with **422 Unprocessable Content**. Spring Framework 7 names this constant `HttpStatus.UNPROCESSABLE_CONTENT`.

The status mapping used in this service: 400 validation or malformed body, 404 unknown customer, 409 out of stock or lost race, 422 domain rule, 500 anything else (with no detail leaked).

## Try it yourself

1. `curl -si localhost:18102/customers/999999/orders` shows a 404 problem with `type` `urn:plainround:problem:customer-not-found`.
2. `curl -s -X POST localhost:18102/customers/1/orders -H 'Content-Type: application/json' -d '{"lines":[{"sku":"a","quantity":1},{"sku":"a","quantity":1}]}'` shows the 422 problem (create a customer first).

3. `./mvnw test -Dtest=OrderControllerTest` asserts the shapes.

## Mistakes people make

- Returning a plain string or a custom error object for each error: clients need a parser per case.
- Leaking the exception message of an unexpected error (a SQL string, a host name): the last-resort handler here answers a generic "The request failed." and logs the details instead.
- Using `detail` as a stable code: it is for people. Use `type` for programs.
- Forgetting that `status` in the body should match the HTTP status line.

## Check yourself

1. Which content type marks a problem document? *`application/problem+json`.*
2. Which field names the kind of problem? *`type`.*
3. How do you add a custom field in Spring? *`problem.setProperty(name, value)`.*
4. Why a generic 500 body? *Not to leak internals.*
5. What does the base class `ResponseEntityExceptionHandler` give you? *Problem documents for the framework's own exceptions.*

## Further reading

- RFC 9457, Problem Details for HTTP APIs: https://www.rfc-editor.org/rfc/rfc9457.html
- Spring Framework 7.0.10, "Error Responses": https://docs.spring.io/spring-framework/reference/7.0.10/web/webmvc/mvc-ann-rest-exceptions.html
