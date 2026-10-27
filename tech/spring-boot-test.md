# Spring Boot Test (test slices, MockMvc, full-context tests)

## What it is

Spring Boot Test is the set of annotations that start a Spring application context inside a test. You choose how much to start:

- **No Spring** (plain JUnit): fastest; tests one class.
- **A slice**, such as `@WebMvcTest`: starts only the web layer (controllers, the error advice, JSON conversion) and replaces the rest with mocks.
- **The full context**, `@SpringBootTest`: starts the whole application, optionally on a real port.

**MockMvc** is a tool that sends a fake HTTP request straight into the controllers, without a network, and lets you assert on the answer.

## Why this repository uses it

The lesson says to prefer slice tests and to prove each status code with a test. A slice starts faster than the full application and fails for web reasons only. A few full-context tests (`*IT`) then prove the pieces fit together.

## Where it appears

| Path | Kind |
| --- | --- |
| `interfaces/web/OrderControllerTest.java`, `CustomerStockAuditControllersTest.java` | `@WebMvcTest` with MockMvc |
| `traps/NPlusOneIT.java`, `traps/OrderModelIT.java` | `@DataJpaTest`: only the JPA layer and a database |
| `traps/SelfInvocationIT.java`, `AuditIT.java`, `CheckoutIT.java`, `OrderServiceScenarioIT.java` | `@SpringBootTest` |

## How it works underneath

1. The Spring extension for JUnit builds an application context for the test class and **caches** it: the next class with the same configuration reuses it, which saves minutes.
2. `@WebMvcTest(X.class)` finds the application class, but loads only web beans: controllers, advice, filters, JSON converters. Services behind the controller are missing, so the test declares them with `@MockitoBean`.
3. `MockMvc` calls the DispatcherServlet directly in the same thread: `mockMvc.perform(post("/books").content(...))` returns a result you check with `status()`, `jsonPath(...)`, `content()`.
4. `@SpringBootTest(webEnvironment = RANDOM_PORT)` starts the real embedded web server on a free port; the test then uses an HTTP client against `localhost:<port>`. The port is available as `${local.server.port}`.
5. `ApplicationContextRunner` builds a context from a few classes and lets you assert that startup fails or succeeds with given properties.
6. The test classpath adds a `config/application.properties` that is merged over the main one (see `properties-files.md`).

## Walkthrough of this repository's use

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final String CHECKOUT = "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":2}]}";
    private static final Order STORED =
            new Order(
                    10,
                    1,
                    Instant.parse("2026-10-27T10:00:00Z"),
                    List.of(new OrderLine("sku-1", 2)));

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CheckoutService checkout;

    @MockitoBean private OrderQueryService queries;
```

`@WebMvcTest(OrderController.class)` loads only the controller and its advice; the two services are mocks. Each test scripts the mock and checks the HTTP answer: 201, 400, 404, 409, 422, 500.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(PostgresContainer.class)
@Import(JpaOrderRepository.class)
class NPlusOneIT {
```

`@DataJpaTest` starts only JPA: entities, repositories, the transaction manager, no web layer. `@AutoConfigureTestDatabase(replace = NONE)` says "do not swap in an in-memory database; use the real one" (see `testcontainers.md`). `@Import(JpaOrderRepository.class)` adds the one adapter under test, because slices do not load `@Repository` beans on their own.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportTestcontainers(PostgresContainer.class)
@Import({SelfInvocationIT.Beans.class})
class SelfInvocationIT {
```

`@SpringBootTest(webEnvironment = NONE)` starts the whole application without a web server. This class deliberately is **not** transactional: a test-wide transaction would do the rolling back and hide the bug the test exists to show.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportTestcontainers(PostgresContainer.class)
class OrderServiceScenarioIT {
```

The scenario test uses `RANDOM_PORT` and calls the service over real HTTP.

**Every test class that starts a Spring context or a slice (`@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`, `ApplicationContextRunner`, `AnnotationConfigApplicationContext`), with its tests:**

- `src/test/java/dev/plainround/orders/OrderServiceScenarioIT.java`:
  - `checkoutListFailAndAuditThroughTheApi`
  - `errorsAreProblemJsonAtTheBoundary`
  - `healthIsUpWithTheDatabaseBehindIt`
- `src/test/java/dev/plainround/orders/interfaces/web/CustomerStockAuditControllersTest.java`:
  - `registerReturns201WithTheNewCustomer`
  - `aBlankNameIs400NamingTheField`
  - `getOfAnUnknownCustomerIs404`
  - `putSetsTheStockLevel`
  - `aNegativeLevelIs400`
  - `getOfASkuWithoutAStockRecordIs409OutOfStock`
  - `auditReturnsTheNewestEntriesAndBoundsTheLimit`
- `src/test/java/dev/plainround/orders/interfaces/web/OrderControllerTest.java`:
  - `checkoutReturns201WithTheStoredOrder`
  - `anEmptyBasketIs400NamingTheLines`
  - `aBadLineIs400NamingTheLineAndField`
  - `anUnknownCustomerIs404`
  - `anOutOfStockLineIs409`
  - `aLostStockRaceIs409NamedConcurrentUpdate`
  - `aRuleTheAnnotationsCannotExpressIs422`
  - `listReturnsTheOrdersWithTheirLines`
  - `aNonNumericCustomerIdIsAProblemDetail400`
  - `anUnexpectedErrorIs500WithoutLeakingTheMessage`
- `src/test/java/dev/plainround/orders/traps/AuditIT.java`:
  - `sameTransactionAuditIsLostWhenCheckoutFails`
  - `auditWithRequiresNewSurvivesRollback`
  - `requiresNewOnTheSameBeanIsIgnored`
- `src/test/java/dev/plainround/orders/traps/CheckoutIT.java`:
  - `checkoutStoresTheOrderWithAllItsLinesAndTakesTheStock`
  - `aLaterLineOutOfStockRollsBackTheOrderAndTheEarlierReservationButKeepsTheAudit`
  - `anUnknownCustomerLeavesOnlyTheAuditEntry`
  - `aTransactionThatReadAStaleStockVersionLosesTheRaceInsteadOfOverwriting`
  - `racingCheckoutsForTheLastUnitsNeverOversell`
- `src/test/java/dev/plainround/orders/traps/NPlusOneIT.java`:
  - `theNaiveListingRunsOneQueryForTheOrdersAndOneMorePerOrderForItsLines`
  - `theNaiveCountKeepsGrowingWith200Orders`
  - `fetchJoinLoadsTwentyOrdersWithTheirLinesInOneStatement`
  - `fetchJoinKeepsQueryCountFlatAt200Orders`
- `src/test/java/dev/plainround/orders/traps/OpenInViewIT.java`:
  - `aLazyLoadOutsideATransactionFailsWhenOpenInViewIsOff`
- `src/test/java/dev/plainround/orders/traps/OpenInViewOnIT.java`:
  - `openInViewOnHidesTheLazyLoadByKeepingTheSessionOpenForTheWholeRequest`
- `src/test/java/dev/plainround/orders/traps/OrderModelIT.java`:
  - `savesAndReadsBackACustomerWithTwoOrdersOfThreeLinesEach`
  - `loadingOneOrderByIdIsExactlyOneStatement`
  - `openInViewIsExplicitlyOffInTheApplicationConfiguration`
- `src/test/java/dev/plainround/orders/traps/SchemaMirrorIT.java`:
  - `theSchemaScriptBuildsTheSameSchemaAsTheMigrations`
- `src/test/java/dev/plainround/orders/traps/SelfInvocationIT.java`:
  - `selfInvocationLeavesAPartialOrderOnFailure`
  - `proxyFixedRollsBackCompletelyOnTheSameFailure`
  - `proxyFixedCommitsTheWholeOrderWhenNothingFails`

## Try it yourself

1. `./mvnw test -Dtest=OrderControllerTest` runs a slice test.
2. `make test-unit` runs everything that needs no Docker; `make test` adds the database tests.
3. Read `target/surefire-reports/` (or `failsafe-reports/`) for the first failing assertion.
4. Search a test for `MockMvc` and compare it with the `curl` command in the README for the same call.

## Mistakes people make

- Using `@SpringBootTest` for everything: slow, and failures are far from the cause.
- Forgetting that a slice does not contain your service beans: the missing `@MockitoBean` shows as "No qualifying bean".
- Putting `@Transactional` on a test of transaction behavior: the test's own transaction changes the result.
- Test settings in a file with the same path as the main one: it hides the main file. Use `config/`.

## Check yourself

1. What does `@WebMvcTest` load? *Only the web layer.*
2. What replaces a service inside a slice? *A `@MockitoBean`.*
3. What does MockMvc avoid? *The network: it calls the dispatcher directly.*
4. Why is a context cached between test classes? *Starting one is slow.*
5. Which annotation gives a test the real server's port? *`@SpringBootTest(webEnvironment = RANDOM_PORT)`, then `${local.server.port}`.*

## Further reading

- Spring Boot 4.1.2, "Testing Spring Boot Applications": https://docs.spring.io/spring-boot/4.1.2/reference/testing/spring-boot-applications.html
- Spring Framework 7.0.10, MockMvc: https://docs.spring.io/spring-framework/reference/7.0.10/testing/mockmvc.html
