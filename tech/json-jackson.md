# JSON and Jackson

## What it is

JSON is a text format for data: objects `{"name": "Dune"}`, lists `[1, 2]`, strings in double quotes, numbers, `true`, `false`, `null`. Jackson is the Java library that converts Java objects to JSON text (**serialization**) and JSON text to Java objects (**deserialization**). Spring Boot 4.1.2 ships Jackson 3, whose packages start with `tools.jackson` (older versions used `com.fasterxml.jackson`).

## Why this repository uses it

Every endpoint receives and returns JSON. Records are used for every request and answer, because Jackson can read and write them directly.

## Where it appears

| Path | Role |
| --- | --- |
| `interfaces/web/*Request.java`, `*Response.java` | the JSON shapes of the API |
| tests | read answers with the JsonPath library (`$.errors[0].field`) |

## How it works underneath

1. Spring MVC has **message converters**. For `application/json` the converter is backed by Jackson.
2. On a request, the converter reads the body bytes and calls Jackson to build the record named by the `@RequestBody` parameter. Unknown fields are ignored by Spring Boot's setup; a missing field becomes `null` (or `0` for `int`).
3. On the answer, Jackson walks the record's components and writes `{"name": value, ...}` using the component names.
4. If the body is not valid JSON, Jackson throws, and Spring turns it into a 400 (see `problem-details.md`).
5. `BigDecimal` is written as a JSON number with its digits kept, so `42.00` stays `42.00`.

## Walkthrough of this repository's use

```java
public record OrderResponse(long id, long customerId, Instant placedAt, List<LineResponse> lines) {

    /**
     * One line of an order.
     *
     * @param sku the product code
     * @param quantity the number of units
     */
    public record LineResponse(String sku, int quantity) {

        static LineResponse from(OrderLine line) {
            return new LineResponse(line.sku(), line.quantity());
        }
    }

    /**
     * Converts a domain order.
     *
     * @param order the order
     * @return its API shape
     */
    public static OrderResponse from(Order order) {
        List<LineResponse> lines = order.lines().stream().map(LineResponse::from).toList();
        return new OrderResponse(order.id(), order.customerId(), order.placedAt(), lines);
    }
}
```

A record can contain another record (`LineResponse`) and a list of them; Jackson follows the structure. `Instant placedAt` is written as an ISO text timestamp such as `2026-10-27T10:00:00Z`. The request side, `CheckoutRequest`, holds a list of `LineRequest`.

## Try it yourself

1. `curl -s localhost:18102/audit | jq .` pretty-prints the JSON (see `jq.md`).
2. Send broken JSON: `curl -si -X POST localhost:18102/customers -H 'Content-Type: application/json' -d '{oops'` and read the 400.
3. In a test, `JsonPath.read(body, "$.errors[0].field")` extracts one value; open a `*ScenarioIT` or `*Test` to see it used.

## Mistakes people make

- Forgetting the `Content-Type: application/json` header: Spring answers 415.
- Using `double` for money: use `BigDecimal` (this repository does for prices).
- Expecting a missing number to be an error: an `int` component becomes 0. The `@Min(1)` rule on `pages` or `quantity` catches that.
- Trailing commas or single quotes: they are not JSON.

## Check yourself

1. What are serialization and deserialization? *Object to JSON text, and JSON text to object.*
2. How do record components become JSON keys? *By their names.*
3. What happens to invalid JSON? *Jackson fails and Spring answers 400.*
4. Why use `BigDecimal` for prices? *It keeps exact decimals.*
5. What do the `tools.jackson` packages tell you? *The project uses Jackson 3.*

## Further reading

- JSON: https://www.json.org/
- Spring Boot 4.1.2, "JSON": https://docs.spring.io/spring-boot/4.1.2/reference/features/json.html
- Jackson project: https://github.com/FasterXML/jackson
