# JUnit (Jupiter)

## What it is

JUnit is the standard framework for writing automated tests in Java. A **test** is a method that runs some code and checks the result; if a check fails, the test fails. JUnit 5 (also called Jupiter) finds the test methods, runs them, and reports the results. Spring Boot 4.1.2 manages JUnit 6.0.3. JUnit 6 keeps the JUnit 5 programming model (the "Jupiter" API), so the annotations used here (`@Test`, `@ParameterizedTest`, `@BeforeEach`, `@AfterEach`) are the ones taught as "JUnit 5".

## Why this repository uses it

The lesson's "Done when" lines are statements a test can prove. Test names are written as sentences that describe behavior (`blankTitleReturns400NamingTheField`), so a failing test name already tells you what broke.

## Where it appears

| Path | Role |
| --- | --- |
| `src/test/java/dev/plainround/orders/domain/`, `application/`, `interfaces/web/` | fast tests with no database |
| `src/test/java/dev/plainround/orders/traps/*IT.java` | tests that need the real database (the three traps) |
| `pom.xml` | Surefire runs `*Test`; Failsafe runs `*IT` |

## How it works underneath

1. Maven's Surefire plugin starts a JVM and asks JUnit to find every class whose name matches `*Test`. Failsafe does the same for `*IT` later in the build.
2. For each test method (marked `@Test`) JUnit creates a **new instance** of the test class, runs `@BeforeEach` methods, runs the test, runs `@AfterEach` methods. So tests do not share fields.
3. A test passes if the method returns normally and fails if it throws. Assertion libraries (see `assertj.md`) throw when a check fails.
4. `@ParameterizedTest` runs the same method several times with different inputs.
5. Extensions add behavior. Spring's test support is a JUnit extension that builds an application context for the class.

## Walkthrough of this repository's use

```java
class OrderLineTest {

    @Test
    void aLineKeepsItsSkuAndQuantity() {
        OrderLine line = new OrderLine("sku-1", 3);

        assertThat(line.sku()).isEqualTo("sku-1");
        assertThat(line.quantity()).isEqualTo(3);
    }
```

A package-private class, a `@Test` method with a sentence for a name, and the body in the order arrange, act, assert.

```java
@ParameterizedTest
@ValueSource(ints = {0, -1, 1001})
void quantitiesOutsideOneToAThousandAreRejected(int quantity) {
    assertThatThrownBy(() -> new OrderLine("sku-1", quantity))
            .isInstanceOf(InvalidOrder.class)
            .hasMessageContaining("quantity");
}
```

`@ParameterizedTest` with `@ValueSource(ints = {0, -1, 1001})` runs the method three times, once per number, each expecting `InvalidOrder`.

```java
@AfterEach
void clean() {
    DatabaseReset.truncate(jdbc);
}
```

`@AfterEach` runs after every test method in that class: here it empties the database so the next test starts clean.

**Every test class in this repository, with the test methods JUnit runs in it:**

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

1. `./mvnw test` runs the `*Test` classes and prints one line per class.
2. `./mvnw test -Dtest=OrderLineTest` runs one class.
3. `./mvnw test -Dtest='OrderLineTest#aLineKeepsItsSkuAndQuantity'` runs one method.
4. Open `target/surefire-reports/*.txt` for the results in text.
5. Change an expected value in a copy of a test and watch the failure message.

## Mistakes people make

- Tests that depend on the order of other tests or share state: JUnit may run them in any order.
- Names such as `test1`: when it fails you learn nothing.
- Asserting nothing, or asserting the same thing the code under test computed.
- Sleeping to wait for something: use a loop with a deadline or a fake clock.

## Check yourself

1. What makes a test fail? *An exception, usually from a failed assertion.*
2. Does a field set in one test survive to the next? *No, each test gets a new instance.*
3. What does `@ParameterizedTest` do? *Runs the method once per input.*
4. Which plugin runs `*IT` classes? *Failsafe.*
5. Why are test names sentences? *A failure explains itself.*

## Further reading

- JUnit User Guide: https://docs.junit.org/current/user-guide/
- Spring Boot 4.1.2, "Testing": https://docs.spring.io/spring-boot/4.1.2/reference/testing/index.html
