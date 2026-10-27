# Java 25

## What it is

Java is a programming language. You write source code in `.java` files. A tool called the **compiler** (`javac`) turns each file into **bytecode** (`.class` files). A program called the **JVM** (Java Virtual Machine, started by the `java` command) reads the bytecode and runs it. A **JDK** (Java Development Kit) is the compiler, the JVM and the standard library in one download. A **JRE** is the JVM and the standard library without the compiler.

## Why this repository uses it

The lesson is about Spring, and Spring is a Java framework. This repository is written for Java 25: the lesson names no version, so the repository uses the current long-term-support release.

## Where it appears

| Path | What it does with Java |
| --- | --- |
| `src/main/java/dev/plainround/orders/domain/` | records for the business data (`Order`, `OrderLine`, `OrderDraft`), the named errors and the interfaces ("ports") the application needs |
| `src/main/java/.../application/` | the use cases, `CheckoutService` first among them |
| `src/main/java/.../infrastructure/persistence/` | the database entities and the classes that talk to them |
| `src/main/java/.../interfaces/web/` | the HTTP controllers |
| `src/test/java/.../traps/` | the three traps, written as tests |

## How it works underneath

A few more words used below. A **thread** is one line of execution inside a running program; a program can run many at once, and a web server uses one per request. An **exception** is an object a method throws when something goes wrong; it travels up the chain of callers until some code catches it, and if none does the thread ends with an error. **`null`** is the value of a reference that points to nothing. A **generic type** such as `List<Book>` is a list that holds only books; the part in angle brackets is a type parameter. `static` means "belongs to the class, not to one object"; `final` on a field means "set once, never changed"; `private` means "visible only inside this class". `Optional<T>` is a box that holds a value or is empty, used instead of `null` for "maybe nothing".

1. `javac` reads `.java` files and writes `.class` files. In this repository Maven calls it for you.
2. A **package** is a folder of classes. The line `package dev.plainround.orders.domain;` at the top of a file says which folder the class belongs to.
3. A **class** describes a kind of object: its data (fields) and its actions (methods). A **record** is a short form of a class that only holds data and cannot change after it is built. An **interface** lists methods without writing their code; other classes promise to provide them.
4. At run time the JVM loads classes when they are first needed, runs `public static void main(String[] args)` of the class you name, and frees memory that nothing points to any more (**garbage collection**).
5. An **annotation** such as `@Override` or `@Test` is a label written in front of a class or method. The compiler, a test runner or a framework reads the label and acts on it.
6. A **lambda** such as `line -> line.sku()` is a small function written inline. A **stream** is a pipeline that applies lambdas to a list: `list.stream().map(...).toList()`.

## Walkthrough of this repository's use

**Records.** A record declares its fields in the header and gets the constructor, the accessors and `equals`, `hashCode` and `toString` for free.

```java
public record OrderLine(String sku, int quantity) {

    /** Longest accepted SKU. */
    public static final int MAX_SKU_LENGTH = 64;

    /** Largest quantity on one line. */
    public static final int MAX_QUANTITY = 1000;

    /** Checks the invariants. */
    public OrderLine {
        if (sku == null || sku.isBlank() || sku.length() > MAX_SKU_LENGTH) {
            throw new InvalidOrder("sku must be 1 to " + MAX_SKU_LENGTH + " characters");
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new InvalidOrder("quantity must be between 1 and " + MAX_QUANTITY);
        }
    }
}
```
`public record OrderLine(String sku, int quantity)` declares two fields and gives the accessors `sku()` and `quantity()`. The compact constructor `public OrderLine { ... }` checks the rules (a SKU of 1 to 64 characters, a quantity of 1 to 1000) and throws `InvalidOrder` if they are broken. A record cannot be built in a broken state, so every part of the program can trust one.

**Sealed classes and `switch` patterns.** Not used in this repository: sealed classes and `switch` patterns. The code keeps to records, interfaces, lambdas and streams.

**Interfaces as ports.** `OrderRepository`, `StockRepository` and `AuditLog` are interfaces in `domain`; the classes in `infrastructure.persistence` implement them. The use cases depend on the interfaces only.

**Lambdas and streams.** `JpaOrderRepository.place` uses `draft.lines().forEach(line -> entity.addLine(...))` and `findByCustomerId` uses `stream().map(JpaOrderRepository::toDomain).toList()`.

**Checked and unchecked exceptions.** All the project's errors extend `RuntimeException` (unchecked): the compiler does not force every caller to catch them. Each has a name that says what went wrong, such as `OutOfStock`, and one place (the "advice" class) turns them into HTTP answers.

**Compiler settings.** The build runs `javac` with `-Xlint:all -Werror`: every warning becomes an error. That keeps unchecked casts and deprecated calls out of the code.

**Every Java file in this repository, and what it is.** The descriptions below are the first sentence of each class's own documentation comment (the `/** ... */` block above the class, called Javadoc).

| File | What it is |
| --- | --- |
| `src/main/java/dev/plainround/orders/OrderServiceApplication.java` | Entry point of the order service. |
| `src/main/java/dev/plainround/orders/application/AuditService.java` | Reads the audit trail. |
| `src/main/java/dev/plainround/orders/application/CheckoutService.java` | Places an order: all of it, or none of it. |
| `src/main/java/dev/plainround/orders/application/CustomerService.java` | Registers customers and looks them up. |
| `src/main/java/dev/plainround/orders/application/OrderQueryService.java` | Reads a customer's orders. |
| `src/main/java/dev/plainround/orders/application/StockService.java` | Sets and reads the units on hand. |
| `src/main/java/dev/plainround/orders/domain/AuditEntry.java` | One line of the audit trail. |
| `src/main/java/dev/plainround/orders/domain/AuditLog.java` | Port: the audit trail. |
| `src/main/java/dev/plainround/orders/domain/Customer.java` | A customer who places orders. |
| `src/main/java/dev/plainround/orders/domain/CustomerNotFound.java` | No customer has the requested id. |
| `src/main/java/dev/plainround/orders/domain/CustomerRepository.java` | Port: where customers are kept. |
| `src/main/java/dev/plainround/orders/domain/InvalidCustomer.java` | A customer was built with values that break its invariants. |
| `src/main/java/dev/plainround/orders/domain/InvalidOrder.java` | An order or one of its lines breaks a rule, such as an empty basket or the same SKU twice. |
| `src/main/java/dev/plainround/orders/domain/Order.java` | A stored order with its lines. |
| `src/main/java/dev/plainround/orders/domain/OrderDraft.java` | What a customer asks to buy, before it is stored. |
| `src/main/java/dev/plainround/orders/domain/OrderLine.java` | One line of an order. |
| `src/main/java/dev/plainround/orders/domain/OrderRepository.java` | Port: where orders are kept. |
| `src/main/java/dev/plainround/orders/domain/OutOfStock.java` | A SKU has too few units left, or no stock record at all. |
| `src/main/java/dev/plainround/orders/domain/StockConflict.java` | Another checkout changed the same stock row first; the caller may retry the whole checkout. |
| `src/main/java/dev/plainround/orders/domain/StockRepository.java` | Port: the units on hand per SKU. |
| `src/main/java/dev/plainround/orders/infrastructure/ClockConfig.java` | The one place the real clock is created; tests replace this bean with a fixed one. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/AuditJpaRepository.java` | Spring Data access to the audit trail. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/AuditRecordEntity.java` | The `audit_records` table. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/CustomerEntity.java` | The `customers` table. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/CustomerJpaRepository.java` | Spring Data access to customers. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaAuditLog.java` | Adapter: the audit trail. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaCustomerRepository.java` | Adapter: the customer port on top of Spring Data JPA. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaOrderRepository.java` | Adapter: the order port on top of Spring Data JPA. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/JpaStockRepository.java` | Adapter: the stock port on top of Spring Data JPA, with optimistic locking on every change. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/OrderEntity.java` | The `orders_table` table ("order" is a reserved SQL word). |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/OrderJpaRepository.java` | Spring Data access to orders. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/OrderLineEntity.java` | The `order_lines` table. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/StockItemEntity.java` | The `stock_items` table; `version` makes concurrent updates of one row fail loudly. |
| `src/main/java/dev/plainround/orders/infrastructure/persistence/StockJpaRepository.java` | Spring Data access to stock records. |
| `src/main/java/dev/plainround/orders/interfaces/web/AuditController.java` | HTTP face of the audit trail (read only). |
| `src/main/java/dev/plainround/orders/interfaces/web/AuditResponse.java` | An audit entry as the API shows it. |
| `src/main/java/dev/plainround/orders/interfaces/web/CheckoutRequest.java` | The body of `POST /customers/{id`/orders}. |
| `src/main/java/dev/plainround/orders/interfaces/web/CustomerController.java` | HTTP face of the customers. |
| `src/main/java/dev/plainround/orders/interfaces/web/CustomerResponse.java` | A customer as the API shows it. |
| `src/main/java/dev/plainround/orders/interfaces/web/LineRequest.java` | One line of a checkout request. |
| `src/main/java/dev/plainround/orders/interfaces/web/NewCustomerRequest.java` | The body of `POST /customers`. |
| `src/main/java/dev/plainround/orders/interfaces/web/OrderController.java` | HTTP face of checkout and of a customer's order list. |
| `src/main/java/dev/plainround/orders/interfaces/web/OrderResponse.java` | An order as the API shows it. |
| `src/main/java/dev/plainround/orders/interfaces/web/OrdersExceptionHandler.java` | The one place errors become `application/problem+json`: the service's named errors, validation failures with the failing fields, the framework's own exceptions (through the parent class) and a last resort that never leaks the cause. |
| `src/main/java/dev/plainround/orders/interfaces/web/RequestIdFilter.java` | Gives every request an id for the logs: the caller's `X-Request-Id` when it looks safe, else a new one. |
| `src/main/java/dev/plainround/orders/interfaces/web/StockController.java` | HTTP face of the stock levels. |
| `src/main/java/dev/plainround/orders/interfaces/web/StockRequest.java` | The body of `PUT /stock/{sku`}. |
| `src/main/java/dev/plainround/orders/interfaces/web/StockResponse.java` | The units on hand for a SKU. |

**Every test class, with the tests inside it** (a test method's name is a sentence about behavior; see `junit.md`):

- `src/test/java/dev/plainround/orders/ApplicationConfigurationTest.java`:
  - `openInViewIsOffAndHibernateOnlyValidatesTheSchemaFlywayOwns`
  - `logsAreJsonShutdownIsGracefulAndProbesAreOn`
  - `theDatabasePasswordHasNoDefaultAndEveryDatabaseCallIsBounded`
- `src/test/java/dev/plainround/orders/ArchitectureTest.java`:
  - `layersOnlyDependInward`
  - `domainDependsOnNothingButTheJdk`
  - `applicationKnowsOnlyTheDomainTheJdkLoggingAndTransactions`
  - `persistenceEntitiesStayInsideTheirPackage`
  - `noFieldInjection`
  - `controllersLiveInTheInterfacesLayer`
  - `adaptersLiveInInfrastructure`
  - `noPackageCycles`
  - `noSelfInvocationOfTransactionalMethods`
  - `transactionalIsNeverOnAPrivateMethod`
- `src/test/java/dev/plainround/orders/OrderServiceScenarioIT.java`:
  - `checkoutListFailAndAuditThroughTheApi`
  - `errorsAreProblemJsonAtTheBoundary`
  - `healthIsUpWithTheDatabaseBehindIt`
- `src/test/java/dev/plainround/orders/ReviewChecklistTest.java`:
  - `theChecklistHasFiveLinesAndEveryLineNamesAnExistingTest`
- `src/test/java/dev/plainround/orders/SqlMirrorTest.java`:
  - `sqlMigrationsMirrorTheFlywayMigrations`
- `src/test/java/dev/plainround/orders/application/CheckoutServiceTest.java`:
  - `checkoutAuditsFirstThenPlacesTheOrderThenReservesEveryLine`
  - `anOutOfStockLineStopsTheCheckoutAndLaterLinesAreNotReserved`
  - `anUnknownCustomerStillLeavesTheAuditEntryAndPlacesNothing`
  - `aRepeatedSkuIsRejectedBeforeAnythingIsStored`
- `src/test/java/dev/plainround/orders/application/QueryAndCustomerServicesTest.java`:
  - `registerStoresAValidatedName`
  - `registerRejectsABlankNameBeforeStoring`
  - `requireOfAnUnknownIdIsCustomerNotFound`
  - `listOrdersChecksTheCustomerThenReadsTheOrders`
  - `stockServiceSetsAndReadsLevelsAndNamesAMissingSku`
  - `auditServiceReturnsTheNewestEntries`
- `src/test/java/dev/plainround/orders/domain/CustomerAndOrderTest.java`:
  - `aCustomerNeedsANameOfOneToHundredCharacters`
  - `aStoredOrderKeepsItsOwnCopyOfTheLines`
  - `namedErrorsSayWhatWentWrong`
- `src/test/java/dev/plainround/orders/domain/OrderDraftTest.java`:
  - `aDraftNeedsAtLeastOneLine`
  - `aDraftHasAtMostFiftyLines`
  - `theSameSkuOnTwoLinesIsRejected`
  - `theDraftKeepsItsOwnCopyOfTheLines`
- `src/test/java/dev/plainround/orders/domain/OrderLineTest.java`:
  - `aLineKeepsItsSkuAndQuantity`
  - `quantitiesOutsideOneToAThousandAreRejected`
  - `blankOrOverlongSkusAreRejected`
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
- `src/test/java/dev/plainround/orders/interfaces/web/RequestIdFilterTest.java`:
  - `aSafeIncomingIdIsKeptAndPutInTheLogContextForTheRequestOnly`
  - `anUnsafeIncomingIdIsReplacedSoItCannotForgeLogLines`
  - `aMissingIdIsGenerated`
- `src/test/java/dev/plainround/orders/support/DatabaseReset.java`: a helper used by the tests
- `src/test/java/dev/plainround/orders/support/Evidence.java`: a helper used by the tests
- `src/test/java/dev/plainround/orders/support/PostgresContainer.java`: a helper used by the tests
- `src/test/java/dev/plainround/orders/traps/AuditIT.java`:
  - `sameTransactionAuditIsLostWhenCheckoutFails`
  - `auditWithRequiresNewSurvivesRollback`
  - `requiresNewOnTheSameBeanIsIgnored`
- `src/test/java/dev/plainround/orders/traps/AuditTrapBeans.java`: a helper used by the tests
- `src/test/java/dev/plainround/orders/traps/CheckoutIT.java`:
  - `checkoutStoresTheOrderWithAllItsLinesAndTakesTheStock`
  - `aLaterLineOutOfStockRollsBackTheOrderAndTheEarlierReservationButKeepsTheAudit`
  - `anUnknownCustomerLeavesOnlyTheAuditEntry`
  - `aTransactionThatReadAStaleStockVersionLosesTheRaceInsteadOfOverwriting`
  - `racingCheckoutsForTheLastUnitsNeverOversell`
- `src/test/java/dev/plainround/orders/traps/LazyEndpointConfig.java`: a helper used by the tests
- `src/test/java/dev/plainround/orders/traps/NPlusOneIT.java`:
  - `theNaiveListingRunsOneQueryForTheOrdersAndOneMorePerOrderForItsLines`
  - `theNaiveCountKeepsGrowingWith200Orders`
  - `fetchJoinLoadsTwentyOrdersWithTheirLinesInOneStatement`
  - `fetchJoinKeepsQueryCountFlatAt200Orders`
- `src/test/java/dev/plainround/orders/traps/NaiveOrderJpaRepository.java`: a helper used by the tests
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
- `src/test/java/dev/plainround/orders/traps/SelfInvokingCheckout.java`: a helper used by the tests

## Try it yourself

1. `java -version` prints the JDK you have. This repository needs 25 or newer.
2. `./mvnw -q compile` compiles the code. Afterwards `ls target/classes` shows the `.class` files in the same folders as the packages.
3. `javap -cp target/classes dev.plainround.orders.domain.OrderLine` (from ``) prints the methods the compiler made for the record: the accessors, `equals`, `hashCode`, `toString`.
4. `jshell` starts an interactive prompt; type `record Point(int x, int y) {}` then `new Point(1,2)` to see a record's `toString`.

## Mistakes people make

- Changing a record's data: it cannot be done. Build a new record instead, as `markedRead()` does.
- Using `==` on strings: it compares identity, not content. Use `.equals`.
- Catching `Exception` everywhere: it hides bugs. This repository catches one named type where it can act on it, and the advice class handles the rest in one place.
- Building with an older JDK than the code needs: `./mvnw` says "release version 25 not supported". Install a newer JDK.
- Long methods. The house style here is short methods with names that say what they do.

## Check yourself

1. What is the difference between the JDK and the JRE? *The JDK also has the compiler.*
2. What does a record's compact constructor do? *Runs before the fields are set; used to check values.*
3. Why does `markedRead()` return a new `Book`? *Records cannot change after they are built.*
4. What does `-Werror` do to a compiler warning? *Turns it into an error that fails the build.*
5. What is an interface for, in this repository? *It names what a layer needs (a "port"), so another class can provide it.*

## Further reading

- The Java Language Specification and the `java.base` API for Java 25: https://docs.oracle.com/en/java/javase/25/
- Records: https://docs.oracle.com/en/java/javase/25/language/records.html
