# Spring Web MVC (controllers, filters, error advice)

## What it is

Spring Web MVC is the part of Spring that handles HTTP. A **controller** is a class whose methods answer requests. You mark a method with the URL and the HTTP method it answers (`@GetMapping("/books")`), and Spring calls it, reads the request into the method's parameters, and writes the return value as the response body (as JSON here).

## Why this repository uses it

The lesson is about walking from an HTTP request to a method and back: how input is checked, how an exception becomes an error answer, how a test exercises just this layer. In this project the controllers are thin: all logic is in the use cases behind them, because the lesson is about what happens below.

## Where it appears

| Path | Role |
| --- | --- |
| `interfaces/web/OrderController.java`, `CustomerController.java`, `StockController.java`, `AuditController.java` | the endpoints |
| `interfaces/web/*Request.java`, `*Response.java` | the shapes of requests and answers |
| `interfaces/web/OrdersExceptionHandler.java` | turns the named errors into HTTP answers |
| `interfaces/web/RequestIdFilter.java` | a filter that gives each request an id |

## How it works underneath

1. The embedded Tomcat accepts a TCP connection and parses the HTTP text into a request object.
2. The request passes through the **filter chain**: classes that can look at or change every request and response before and after the rest (here: assign an id).
3. A single servlet called the **DispatcherServlet** picks the controller method whose mapping matches the URL and method.
4. **Argument resolvers** fill the method's parameters: `@PathVariable` from the URL, `@RequestParam` from the query string, `@RequestBody` from the JSON body (parsed by Jackson), and `@Valid` triggers validation.
5. The method runs. Its return value is turned into JSON and written with the status given by `@ResponseStatus` (200 by default).
6. If anything throws, the **exception resolvers** look for a matching `@ExceptionHandler` method in a class marked `@RestControllerAdvice` (one class that serves all controllers) and use its result as the response.

## Walkthrough of this repository's use

```java
@RestController
@RequestMapping("/customers/{customerId}/orders")
public class OrderController {

    private final CheckoutService checkout;
    private final OrderQueryService queries;

    /**
     * Wires the controller.
     *
     * @param checkout the checkout use case
     * @param queries the order queries
     */
    public OrderController(CheckoutService checkout, OrderQueryService queries) {
        this.checkout = checkout;
        this.queries = queries;
    }
```

`@RequestMapping("/customers/{customerId}/orders")` has a **path variable** in curly braces; the methods read it with `@PathVariable long customerId`.

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public OrderResponse checkout(
        @PathVariable long customerId, @Valid @RequestBody CheckoutRequest request) {
    return OrderResponse.from(checkout.checkout(customerId, request.toDomain()));
}
```

`POST /customers/7/orders` with a JSON body: `@Valid @RequestBody CheckoutRequest request` is read and checked; the controller calls `checkout.checkout(customerId, request.toDomain())` and returns the stored order as a response record with status 201. The controller holds no business rule.

The error advice:

```java
@ExceptionHandler(OutOfStock.class)
public ProblemDetail handleOutOfStock(OutOfStock exception) {
    return problem(HttpStatus.CONFLICT, "out-of-stock", "Out of stock", exception.getMessage());
}
```

Each named error has one handler: `CustomerNotFound` 404, `OutOfStock` and `StockConflict` 409, `InvalidOrder` 422, anything else 500 with no detail leaked. The class extends `ResponseEntityExceptionHandler` so the framework's own errors share the same shape (see `problem-details.md`).

The filter `RequestIdFilter` assigns an `X-Request-Id`, puts it in the log context and echoes it in the response, exactly as in the first project.

## Try it yourself

1. After `make up`: `curl -s -X POST localhost:18102/customers -H 'Content-Type: application/json' -d '{"name":"Ada"}'` prints the new customer.
2. `curl -si localhost:18102/customers/999999/orders` shows `404` and `application/problem+json`.
3. `curl -si -X POST localhost:18102/customers -H 'Content-Type: application/json' -d '{"name":""}'` shows `400` and an `errors` list naming `name`.

4. `./mvnw test -Dtest=OrderControllerTest` runs the web-layer tests without starting the whole application.

## Mistakes people make

- Putting business rules in the controller: it becomes impossible to test without HTTP.
- Returning a database entity from a controller: lazy data may blow up while the JSON is written.
- Forgetting `@Valid` on the body parameter: validation silently does not run.
- Using `int` for an optional query parameter: "absent" becomes 0.
- Two advice classes whose handlers both match: the one with the higher precedence wins, and the other is silently ignored. This repository has one advice.

## Check yourself

1. What does the DispatcherServlet do? *Picks the controller method for a request.*
2. What does `@RestController` add to `@Controller`? *`@ResponseBody`: return values become the body.*
3. Where does an exception from a controller go? *To a matching `@ExceptionHandler` in a `@RestControllerAdvice`.*
4. Why does the filter remove the request id in `finally`? *Threads are reused; the id must not leak into the next request.*
5. What does `@PathVariable` read? *A value from the URL path.*

## Further reading

- Spring Framework 7.0.10, "Spring Web MVC": https://docs.spring.io/spring-framework/reference/7.0.10/web/webmvc.html
- Exception handling: https://docs.spring.io/spring-framework/reference/7.0.10/web/webmvc/mvc-controller/ann-exceptionhandler.html
