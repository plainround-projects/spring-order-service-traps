# The test code, class by class

## What it is

A test is a small program that uses the application code and checks that the result is what the author expects. This note prints every test class in the repository (`src/test/java`) exactly as it is, and explains each test method below it.

## Why this repository uses it

The tests are the evidence that the code does what the README says. Each test has a name that reads as a sentence. If a test fails, its name tells you which promise broke.

## Where it appears

All tests are under `src/test/java/dev/plainround/orders/`: unit tests (`*Test`), integration tests (`*IT`) that start PostgreSQL in a container, the trap tests in the `traps` folder that prove each pitfall, and helper classes in `support`. The `*IT` tests need Docker, which is why the README marks them as pending capacity.

## How it works underneath

1. Maven's Surefire plugin finds classes named `*Test`, starts JUnit, and runs every method marked `@Test`. The Failsafe plugin does the same for `*IT` in the `verify` phase.
2. A test method passes if it returns normally and fails if it throws. An assertion such as `assertThat(x).isEqualTo(y)` throws when the check is false.
3. Tests follow three steps: set up the data, do the thing, check the result. Blank lines in the listings separate the steps.
4. Spring tests start a (small or full) Spring context first, so the test can ask for real beans or send fake HTTP requests.

## Walkthrough of this repository's use

Each class is printed whole, followed by a reading guide. The guide's "reads as" sentence is the test name turned into words, and "What the body does" translates each statement into plain words with the original in backticks.

### `src/test/java/dev/plainround/orders/ApplicationConfigurationTest.java`

Package `dev.plainround.orders`. The operability and safety settings live in one file; this test fails if someone changes them by accident.

The whole file, as it is in the repository:

```java
package dev.plainround.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * The operability and safety settings live in one file; this test fails if someone changes them by
 * accident. No Spring context and no Docker: it reads the file the application ships.
 */
class ApplicationConfigurationTest {

    private static Properties main() throws IOException {
        Properties properties = new Properties();
        try (Reader reader =
                Files.newBufferedReader(Path.of("src/main/resources/application.properties"))) {
            properties.load(reader);
        }
        return properties;
    }

    @Test
    void openInViewIsOffAndHibernateOnlyValidatesTheSchemaFlywayOwns() throws IOException {
        Properties properties = main();

        assertThat(properties.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }

    @Test
    void logsAreJsonShutdownIsGracefulAndProbesAreOn() throws IOException {
        Properties properties = main();

        assertThat(properties.getProperty("logging.structured.format.console"))
                .isEqualTo("logstash");
        assertThat(properties.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(properties.getProperty("management.endpoint.health.probes.enabled"))
                .isEqualTo("true");
    }

    @Test
    void theDatabasePasswordHasNoDefaultAndEveryDatabaseCallIsBounded() throws IOException {
        Properties properties = main();

        assertThat(properties.getProperty("spring.datasource.password"))
                .isEqualTo("${POSTGRES_PASSWORD}");
        assertThat(properties.getProperty("spring.datasource.url"))
                .contains("connectTimeout=", "socketTimeout=");
        assertThat(properties.getProperty("spring.datasource.hikari.connection-timeout"))
                .isNotBlank();
        assertThat(properties.getProperty("spring.transaction.default-timeout")).isNotBlank();
    }
}
```

Reading it, one piece at a time:

- **`ApplicationConfigurationTest`**, a class. The operability and safety settings live in one file; this test fails if someone changes them by accident. No Spring context and no Docker: it reads the file the application ships.
  - **method `main`**: the signature is `static Properties main() throws IOException`.
    - Signature: `private static Properties main() throws IOException`
    - What the body does, in order:
      - creates the variable `properties` (of type `Properties`) holding `new Properties()`
      - tries this (managing `Reader reader = Files.newBufferedReader(Path.of("src/main/resources/application.properties"))` so it is closed afterwards):
        - calls `properties.load(reader)`
      - returns `properties`
  - **test `openInViewIsOffAndHibernateOnlyValidatesTheSchemaFlywayOwns`**: reads as "open in view is off and hibernate only validates the schema flyway owns"
    - Signature: `void openInViewIsOffAndHibernateOnlyValidatesTheSchemaFlywayOwns() throws IOException`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `properties` (of type `Properties`) holding `main()`
      - checks that `properties.getProperty("spring.jpa.open-in-view")` equals `"false"`
      - checks that `properties.getProperty("spring.jpa.hibernate.ddl-auto")` equals `"validate"`
  - **test `logsAreJsonShutdownIsGracefulAndProbesAreOn`**: reads as "logs are json shutdown is graceful and probes are on"
    - Signature: `void logsAreJsonShutdownIsGracefulAndProbesAreOn() throws IOException`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `properties` (of type `Properties`) holding `main()`
      - checks that `properties.getProperty("logging.structured.format.console")` equals `"logstash"`
      - checks that `properties.getProperty("server.shutdown")` equals `"graceful"`
      - checks that `properties.getProperty("management.endpoint.health.probes.enabled")` equals `"true"`
  - **test `theDatabasePasswordHasNoDefaultAndEveryDatabaseCallIsBounded`**: reads as "the database password has no default and every database call is bounded"
    - Signature: `void theDatabasePasswordHasNoDefaultAndEveryDatabaseCallIsBounded() throws IOException`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `properties` (of type `Properties`) holding `main()`
      - checks that `properties.getProperty("spring.datasource.password")` equals `"${POSTGRES_PASSWORD}"`
      - checks that `properties.getProperty("spring.datasource.url")` contains `"connectTimeout=", "socketTimeout="`
      - checks that `properties.getProperty("spring.datasource.hikari.connection-timeout")` is not blank
      - checks that `properties.getProperty("spring.transaction.default-timeout")` is not blank

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **throws**: `throws X` in a signature says the method may fail with exception `X`; an exception is an object that stops normal flow and travels up the call stack until something catches it.
- **try/catch**: `try { ... } catch (X e) { ... }` runs the first block and, if it throws `X`, runs the second.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.


### `src/test/java/dev/plainround/orders/ArchitectureTest.java`

Package `dev.plainround.orders`. The dependency rule and the two proxy rules from the lesson, as code: they fail the build, not the review.

The whole file, as it is in the repository:

```java
package dev.plainround.orders;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.transaction.annotation.Transactional;

/**
 * The dependency rule and the two proxy rules from the lesson, as code: they fail the build, not
 * the review.
 */
@AnalyzeClasses(
        packages = "dev.plainround.orders",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layersOnlyDependInward =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Domain")
                    .definedBy("..domain..")
                    .layer("Application")
                    .definedBy("..application..")
                    .layer("Infrastructure")
                    .definedBy("..infrastructure..")
                    .layer("Interfaces")
                    .definedBy("..interfaces..")
                    .whereLayer("Interfaces")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Infrastructure")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Application")
                    .mayOnlyBeAccessedByLayers("Interfaces")
                    .whereLayer("Domain")
                    .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Interfaces");

    @ArchTest
    static final ArchRule domainDependsOnNothingButTheJdk =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideOutsideOfPackages("..domain..", "java..");

    @ArchTest
    static final ArchRule applicationKnowsOnlyTheDomainTheJdkLoggingAndTransactions =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideOutsideOfPackages(
                            "..domain..",
                            "..application..",
                            "java..",
                            "org.slf4j..",
                            "org.springframework.stereotype..",
                            "org.springframework.transaction.annotation..");

    @ArchTest
    static final ArchRule persistenceEntitiesStayInsideTheirPackage =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("..infrastructure.persistence..")
                    .should()
                    .dependOnClassesThat(
                            JavaClass.Predicates.resideInAPackage("..infrastructure.persistence..")
                                    .and(JavaClass.Predicates.simpleNameEndingWith("Entity")));

    @ArchTest
    static final ArchRule noFieldInjection =
            noFields()
                    .should()
                    .beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .orShould()
                    .beAnnotatedWith("jakarta.inject.Inject")
                    .orShould()
                    .beAnnotatedWith("jakarta.annotation.Resource");

    @ArchTest
    static final ArchRule controllersLiveInTheInterfacesLayer =
            classes()
                    .that()
                    .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should()
                    .resideInAPackage("..interfaces..");

    @ArchTest
    static final ArchRule adaptersLiveInInfrastructure =
            classes()
                    .that()
                    .areAnnotatedWith("org.springframework.stereotype.Repository")
                    .should()
                    .resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule noPackageCycles =
            slices().matching("dev.plainround.orders.(*)..").should().beFreeOfCycles();

    /**
     * Trap 2: a call from one method of a class to a {@code @Transactional} method of the same
     * class skips the proxy.
     */
    @ArchTest
    static final ArchRule noSelfInvocationOfTransactionalMethods =
            noClasses().should(callATransactionalMethodOfTheirOwnClass());

    /** A private method is never proxied, so a {@code @Transactional} on it is silently ignored. */
    @ArchTest
    static final ArchRule transactionalIsNeverOnAPrivateMethod =
            methods().that().arePrivate().should().notBeAnnotatedWith(Transactional.class);

    private static ArchCondition<JavaClass> callATransactionalMethodOfTheirOwnClass() {
        return new ArchCondition<>("call a @Transactional method of their own class") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaMethodCall call : javaClass.getMethodCallsFromSelf()) {
                    boolean sameClass = call.getTargetOwner().equals(javaClass);
                    boolean transactional =
                            call.getTarget()
                                    .resolveMember()
                                    .map(method -> method.isAnnotatedWith(Transactional.class))
                                    .orElse(false);
                    if (sameClass && transactional) {
                        events.add(SimpleConditionEvent.satisfied(call, call.getDescription()));
                    }
                }
            }
        };
    }
}
```

Reading it, one piece at a time:

- **`ArchitectureTest`**, a class. The dependency rule and the two proxy rules from the lesson, as code: they fail the build, not the review.
  - annotation `@AnalyzeClasses( packages = "dev.plainround.orders", importOptions = ImportOption.DoNotIncludeTests.class)`: ArchUnit: says which compiled classes the rules in this class check
  - **field `layersOnlyDependInward`** of type `ArchRule`, starting as `layeredArchitecture() .consideringOnlyDependenciesInLayers() .layer("Domain") .definedBy("..domain..") .layer("Application") .definedBy("..application..") .layer("Infrastructure") .definedBy("..infrastructure..") .layer("Interfaces") .definedBy("..interfaces..") .whereLayer("Interfaces") .mayNotBeAccessedByAnyLayer() .whereLayer("Infrastructure") .mayNotBeAccessedByAnyLayer() .whereLayer("Application") .mayOnlyBeAccessedByLayers("Interfaces") .whereLayer("Domain") .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Interfaces")`
    - Declaration: `static final ArchRule layersOnlyDependInward = layeredArchitecture() .consideringOnlyDependenciesInLayers() .layer("Domain") .definedBy("..domain..") .layer("Application") .definedBy("..application..") .layer("Infrastructure") .definedBy("..infrastructure..") .layer("Interfaces") .definedBy("..interfaces..") .whereLayer("Interfaces") .mayNotBeAccessedByAnyLayer() .whereLayer("Infrastructure") .mayNotBeAccessedByAnyLayer() .whereLayer("Application") .mayOnlyBeAccessedByLayers("Interfaces") .whereLayer("Domain") .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Interfaces")`
    - annotation `@ArchTest`: marks a field or method as one architecture rule that ArchUnit checks when the test class runs
  - **field `domainDependsOnNothingButTheJdk`** of type `ArchRule`, starting as `noClasses() .that() .resideInAPackage("..domain..") .should() .dependOnClassesThat() .resideOutsideOfPackages("..domain..", "java..")`
    - Declaration: `static final ArchRule domainDependsOnNothingButTheJdk = noClasses() .that() .resideInAPackage("..domain..") .should() .dependOnClassesThat() .resideOutsideOfPackages("..domain..", "java..")`
    - annotation `@ArchTest`: marks a field or method as one architecture rule that ArchUnit checks when the test class runs
  - **field `applicationKnowsOnlyTheDomainTheJdkLoggingAndTransactions`** of type `ArchRule`, starting as `noClasses() .that() .resideInAPackage("..application..") .should() .dependOnClassesThat() .resideOutsideOfPackages( "..domain..", "..application..", "java..", "org.slf4j..", "org.springframework.stereotype..", "org.springframework.transaction.annotation..")`
    - Declaration: `static final ArchRule applicationKnowsOnlyTheDomainTheJdkLoggingAndTransactions = noClasses() .that() .resideInAPackage("..application..") .should() .dependOnClassesThat() .resideOutsideOfPackages( "..domain..", "..application..", "java..", "org.slf4j..", "org.springframework.stereotype..", "org.springframework.transaction.annotation..")`
    - annotation `@ArchTest`: marks a field or method as one architecture rule that ArchUnit checks when the test class runs
  - **field `persistenceEntitiesStayInsideTheirPackage`** of type `ArchRule`, starting as `noClasses() .that() .resideOutsideOfPackage("..infrastructure.persistence..") .should() .dependOnClassesThat( JavaClass.Predicates.resideInAPackage("..infrastructure.persistence..") .and(JavaClass.Predicates.simpleNameEndingWith("Entity")))`
    - Declaration: `static final ArchRule persistenceEntitiesStayInsideTheirPackage = noClasses() .that() .resideOutsideOfPackage("..infrastructure.persistence..") .should() .dependOnClassesThat( JavaClass.Predicates.resideInAPackage("..infrastructure.persistence..") .and(JavaClass.Predicates.simpleNameEndingWith("Entity")))`
    - annotation `@ArchTest`: marks a field or method as one architecture rule that ArchUnit checks when the test class runs
  - **field `noFieldInjection`** of type `ArchRule`, starting as `noFields() .should() .beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired") .orShould() .beAnnotatedWith("jakarta.inject.Inject") .orShould() .beAnnotatedWith("jakarta.annotation.Resource")`
    - Declaration: `static final ArchRule noFieldInjection = noFields() .should() .beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired") .orShould() .beAnnotatedWith("jakarta.inject.Inject") .orShould() .beAnnotatedWith("jakarta.annotation.Resource")`
    - annotation `@ArchTest`: marks a field or method as one architecture rule that ArchUnit checks when the test class runs
  - **field `controllersLiveInTheInterfacesLayer`** of type `ArchRule`, starting as `classes() .that() .areAnnotatedWith("org.springframework.web.bind.annotation.RestController") .should() .resideInAPackage("..interfaces..")`
    - Declaration: `static final ArchRule controllersLiveInTheInterfacesLayer = classes() .that() .areAnnotatedWith("org.springframework.web.bind.annotation.RestController") .should() .resideInAPackage("..interfaces..")`
    - annotation `@ArchTest`: marks a field or method as one architecture rule that ArchUnit checks when the test class runs
  - **field `adaptersLiveInInfrastructure`** of type `ArchRule`, starting as `classes() .that() .areAnnotatedWith("org.springframework.stereotype.Repository") .should() .resideInAPackage("..infrastructure..")`
    - Declaration: `static final ArchRule adaptersLiveInInfrastructure = classes() .that() .areAnnotatedWith("org.springframework.stereotype.Repository") .should() .resideInAPackage("..infrastructure..")`
    - annotation `@ArchTest`: marks a field or method as one architecture rule that ArchUnit checks when the test class runs
  - **field `noPackageCycles`** of type `ArchRule`, starting as `slices().matching("dev.plainround.orders.(*)..").should().beFreeOfCycles()`
    - Declaration: `static final ArchRule noPackageCycles = slices().matching("dev.plainround.orders.(*)..").should().beFreeOfCycles()`
    - annotation `@ArchTest`: marks a field or method as one architecture rule that ArchUnit checks when the test class runs
  - **field `noSelfInvocationOfTransactionalMethods`** of type `ArchRule`: Trap 2: a call from one method of a class to a `@Transactional` method of the same class skips the proxy.
    - Declaration: `static final ArchRule noSelfInvocationOfTransactionalMethods = noClasses().should(callATransactionalMethodOfTheirOwnClass())`
    - annotation `@ArchTest`: marks a field or method as one architecture rule that ArchUnit checks when the test class runs
  - **field `transactionalIsNeverOnAPrivateMethod`** of type `ArchRule`: A private method is never proxied, so a `@Transactional` on it is silently ignored.
    - Declaration: `static final ArchRule transactionalIsNeverOnAPrivateMethod = methods().that().arePrivate().should().notBeAnnotatedWith(Transactional.class)`
    - annotation `@ArchTest`: marks a field or method as one architecture rule that ArchUnit checks when the test class runs
  - **method `callATransactionalMethodOfTheirOwnClass`**: the signature is `static ArchCondition<JavaClass> callATransactionalMethodOfTheirOwnClass()`.
    - Signature: `private static ArchCondition<JavaClass> callATransactionalMethodOfTheirOwnClass()`
    - What the body does, in order:
      - returns `new ArchCondition<>("call a @Transactional method of their own class") { @Override public void check(JavaClass javaClass, ConditionEvents events) { for (JavaMethodCall call : javaClass.getMethodCallsFromSelf()) { boolean sameClass = call.getTargetOwner().equals(javaClass); boolean transactional = call.getTarget() .resolveMember() .map(method -> method.isAnnotatedWith(Transactional.class)) .orElse(false); if (sameClass && transactional) { events.add(SimpleConditionEvent.satisfied(call, call.getDescription())); } } } }`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@AnalyzeClasses`: ArchUnit: says which compiled classes the rules in this class check.
- `@ArchTest`: marks a field or method as one architecture rule that ArchUnit checks when the test class runs.
- `@Override`: says this method replaces one with the same name from a parent class or interface; the compiler checks that such a parent method exists.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.


### `src/test/java/dev/plainround/orders/OrderServiceScenarioIT.java`

Package `dev.plainround.orders`. The lesson's whole scenario over HTTP against a real server and a real PostgreSQL.

The whole file, as it is in the repository:

```java
package dev.plainround.orders;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import dev.plainround.orders.support.DatabaseReset;
import dev.plainround.orders.support.PostgresContainer;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/** The lesson's whole scenario over HTTP against a real server and a real PostgreSQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportTestcontainers(PostgresContainer.class)
class OrderServiceScenarioIT {

    @Value("${local.server.port}")
    private int port;

    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void checkoutListFailAndAuditThroughTheApi() {
        long customerId =
                JsonPath.<Integer>read(post("/customers", "{\"name\":\"Ada\"}").getBody(), "$.id");
        put("/stock/sku-1", "{\"available\":5}");
        put("/stock/sku-2", "{\"available\":0}");

        ResponseEntity<String> placed =
                post(orders(customerId), "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":2}]}");
        assertThat(placed.getStatusCode().value()).isEqualTo(201);

        String listed = get(orders(customerId)).getBody();
        assertThat(JsonPath.<List<?>>read(listed, "$")).hasSize(1);
        assertThat(JsonPath.<Integer>read(listed, "$[0].lines[0].quantity")).isEqualTo(2);

        ResponseEntity<String> shortage =
                post(
                        orders(customerId),
                        "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":1},{\"sku\":\"sku-2\",\"quantity\":1}]}");
        assertThat(shortage.getStatusCode().value()).isEqualTo(409);
        assertThat(shortage.getHeaders().getContentType().toString())
                .startsWith("application/problem+json");
        assertThat(JsonPath.<String>read(shortage.getBody(), "$.title")).isEqualTo("Out of stock");

        assertThat(JsonPath.<List<?>>read(get(orders(customerId)).getBody(), "$"))
                .as("still one order")
                .hasSize(1);
        assertThat(JsonPath.<Integer>read(get("/stock/sku-1").getBody(), "$.available"))
                .isEqualTo(3);
        assertThat(JsonPath.<List<?>>read(get("/audit").getBody(), "$"))
                .as("both attempts are on record")
                .hasSize(2);
    }

    @Test
    void errorsAreProblemJsonAtTheBoundary() {
        ResponseEntity<String> badBody = post("/customers", "{\"name\":\"\"}");
        ResponseEntity<String> unknown = get("/customers/9999/orders");
        ResponseEntity<String> malformed = post("/customers", "{not json");
        ResponseEntity<String> repeatedSku =
                post(
                        orders(createCustomer()),
                        "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":1},{\"sku\":\"sku-1\",\"quantity\":1}]}");

        assertThat(badBody.getStatusCode().value()).isEqualTo(400);
        assertThat(JsonPath.<String>read(badBody.getBody(), "$.errors[0].field")).isEqualTo("name");
        assertThat(unknown.getStatusCode().value()).isEqualTo(404);
        assertThat(malformed.getStatusCode().value()).isEqualTo(400);
        assertThat(repeatedSku.getStatusCode().value()).isEqualTo(422);
        for (ResponseEntity<String> response : List.of(badBody, unknown, malformed, repeatedSku)) {
            assertThat(response.getHeaders().getContentType().toString())
                    .startsWith("application/problem+json");
        }
    }

    @Test
    void healthIsUpWithTheDatabaseBehindIt() {
        assertThat(get("/actuator/health").getBody()).contains("\"status\":\"UP\"");
        assertThat(get("/actuator/health/readiness").getBody()).contains("UP");
        assertThat(get("/actuator/health").getHeaders().getFirst("X-Request-Id")).isNotBlank();
    }

    private long createCustomer() {
        return JsonPath.<Integer>read(post("/customers", "{\"name\":\"Ada\"}").getBody(), "$.id");
    }

    private static String orders(long customerId) {
        return "/customers/" + customerId + "/orders";
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {})
                .build();
    }

    private ResponseEntity<String> get(String path) {
        return client().get().uri(path).retrieve().toEntity(String.class);
    }

    private ResponseEntity<String> post(String path, String body) {
        return client().post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    private ResponseEntity<String> put(String path, String body) {
        return client().put()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }
}
```

Reading it, one piece at a time:

- **`OrderServiceScenarioIT`**, a class. The lesson's whole scenario over HTTP against a real server and a real PostgreSQL.
  - annotation `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`: starts the whole application inside the test (all beans, the real configuration) so the test can use it
  - annotation `@ImportTestcontainers(PostgresContainer.class)`: starts the containers declared in the named class for this test and wires them into the Spring configuration
  - **method `port`**: the signature is `int port`.
    - Signature: `private int port`
    - annotation `@Value("${local.server.port}")`: injects a value from the configuration into this field or parameter
  - **field `jdbc`** of type `JdbcTemplate`, set once when the object is built.
    - Declaration: `private JdbcTemplate jdbc`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **method `clean`**: the signature is `void clean()`.
    - Signature: `void clean()`
    - annotation `@AfterEach`: runs this method after every test, to clean up
    - What the body does, in order:
      - calls `DatabaseReset.truncate(jdbc)`
  - **test `checkoutListFailAndAuditThroughTheApi`**: reads as "checkout list fail and audit through the api"
    - Signature: `void checkoutListFailAndAuditThroughTheApi()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customerId` (of type `long`) holding `JsonPath.<Integer>read(post("/customers", "{\"name\":\"Ada\"}").getBody(), "$.id")`
      - calls `put("/stock/sku-1", "{\"available\":5}")`
      - calls `put("/stock/sku-2", "{\"available\":0}")`
      - creates the variable `placed` (of type `ResponseEntity<String>`) holding `post(orders(customerId), "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":2}]}")`
      - checks that `placed.getStatusCode().value()` equals `201`
      - creates the variable `listed` (of type `String`) holding `get(orders(customerId)).getBody()`
      - checks that `JsonPath.<List<?>>read(listed, "$")` has the size `1`
      - checks that `JsonPath.<Integer>read(listed, "$[0].lines[0].quantity")` equals `2`
      - creates the variable `shortage` (of type `ResponseEntity<String>`) holding `post( orders(customerId), "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":1},{\"sku\":\"sku-2\",\"quantity\":1}]}")`
      - checks that `shortage.getStatusCode().value()` equals `409`
      - checks that `shortage.getHeaders().getContentType().toString()` starts with `"application/problem+json"`
      - checks that `JsonPath.<String>read(shortage.getBody(), "$.title")` equals `"Out of stock"`
      - checks that `JsonPath.<List<?>>read(get(orders(customerId)).getBody(), "$")` as `"still one order"`
      - and that it has the size `1`
      - checks that `JsonPath.<Integer>read(get("/stock/sku-1").getBody(), "$.available")` equals `3`
      - checks that `JsonPath.<List<?>>read(get("/audit").getBody(), "$")` as `"both attempts are on record"`
      - and that it has the size `2`
  - **test `errorsAreProblemJsonAtTheBoundary`**: reads as "errors are problem json at the boundary"
    - Signature: `void errorsAreProblemJsonAtTheBoundary()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `badBody` (of type `ResponseEntity<String>`) holding `post("/customers", "{\"name\":\"\"}")`
      - creates the variable `unknown` (of type `ResponseEntity<String>`) holding `get("/customers/9999/orders")`
      - creates the variable `malformed` (of type `ResponseEntity<String>`) holding `post("/customers", "{not json")`
      - creates the variable `repeatedSku` (of type `ResponseEntity<String>`) holding `post( orders(createCustomer()), "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":1},{\"sku\":\"sku-1\",\"quantity\":1}]}")`
      - checks that `badBody.getStatusCode().value()` equals `400`
      - checks that `JsonPath.<String>read(badBody.getBody(), "$.errors[0].field")` equals `"name"`
      - checks that `unknown.getStatusCode().value()` equals `404`
      - checks that `malformed.getStatusCode().value()` equals `400`
      - checks that `repeatedSku.getStatusCode().value()` equals `422`
      - repeats for `ResponseEntity<String> response : List.of(badBody, unknown, malformed, repeatedSku)`:
        - checks that `response.getHeaders().getContentType().toString()` starts with `"application/problem+json"`
  - **test `healthIsUpWithTheDatabaseBehindIt`**: reads as "health is up with the database behind it"
    - Signature: `void healthIsUpWithTheDatabaseBehindIt()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - checks that `get("/actuator/health").getBody()` contains `"\"status\":\"UP\""`
      - checks that `get("/actuator/health/readiness").getBody()` contains `"UP"`
      - checks that `get("/actuator/health").getHeaders().getFirst("X-Request-Id")` is not blank
  - **method `createCustomer`**: the signature is `long createCustomer()`.
    - Signature: `private long createCustomer()`
    - What the body does, in order:
      - returns `JsonPath.<Integer>read(post("/customers", "{\"name\":\"Ada\"}").getBody(), "$.id")`
  - **method `orders`**: the signature is `static String orders(long customerId)`.
    - Signature: `private static String orders(long customerId)`
    - What the body does, in order:
      - returns `"/customers/" + customerId + "/orders"`
  - **method `client`**: the signature is `RestClient client()`.
    - Signature: `private RestClient client()`
    - What the body does, in order:
      - returns `RestClient.builder() .baseUrl("http://localhost:" + port) .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {}) .build()`
  - **method `get`**: the signature is `ResponseEntity<String> get(String path)`.
    - Signature: `private ResponseEntity<String> get(String path)`
    - What the body does, in order:
      - returns `client().get().uri(path).retrieve().toEntity(String.class)`
  - **method `post`**: the signature is `ResponseEntity<String> post(String path, String body)`.
    - Signature: `private ResponseEntity<String> post(String path, String body)`
    - What the body does, in order:
      - returns `client().post() .uri(path) .contentType(MediaType.APPLICATION_JSON) .body(body) .retrieve() .toEntity(String.class)`
  - **method `put`**: the signature is `ResponseEntity<String> put(String path, String body)`.
    - Signature: `private ResponseEntity<String> put(String path, String body)`
    - What the body does, in order:
      - returns `client().put() .uri(path) .contentType(MediaType.APPLICATION_JSON) .body(body) .retrieve() .toEntity(String.class)`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@SpringBootTest`: starts the whole application inside the test (all beans, the real configuration) so the test can use it.
- `@ImportTestcontainers`: starts the containers declared in the named class for this test and wires them into the Spring configuration.
- `@Value`: injects a value from the configuration into this field or parameter.
- `@Autowired`: asks Spring to supply the object this field or parameter needs.
- `@AfterEach`: runs this method after every test, to clean up.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **method reference (`::`)**: `Type::method` is a short way to pass an existing method where a function is expected.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.


### `src/test/java/dev/plainround/orders/ReviewChecklistTest.java`

Package `dev.plainround.orders`. Step 6's Done when: every line of the review checklist points at a test that exists.

The whole file, as it is in the repository:

```java
package dev.plainround.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Step 6's Done when: every line of the review checklist points at a test that exists. A reference
 * such as {@code `NPlusOneIT#fetchJoinKeepsQueryCountFlatAt200Orders`} must name a real class and a
 * real method.
 */
class ReviewChecklistTest {

    private static final Pattern REFERENCE = Pattern.compile("`([A-Za-z0-9]+)#([A-Za-z0-9]+)`");
    private static final Path TESTS = Path.of("src/test/java");

    @Test
    void theChecklistHasFiveLinesAndEveryLineNamesAnExistingTest() throws IOException {
        List<String> lines =
                Files.readAllLines(Path.of("docs/REVIEW-CHECKLIST.md")).stream()
                        .filter(line -> line.matches("^\\d\\. .*"))
                        .toList();

        assertThat(lines).hasSize(5);
        List<String> missing = new ArrayList<>();
        for (String line : lines) {
            Matcher matcher = REFERENCE.matcher(line);
            boolean found = false;
            while (matcher.find()) {
                found = true;
                if (!testMethodExists(matcher.group(1), matcher.group(2))) {
                    missing.add(matcher.group(1) + "#" + matcher.group(2));
                }
            }
            assertThat(found).as("line names a test: %s", line).isTrue();
        }
        assertThat(missing).as("referenced tests that do not exist").isEmpty();
    }

    private static boolean testMethodExists(String className, String methodName)
            throws IOException {
        try (Stream<Path> files = Files.walk(TESTS)) {
            Path source =
                    files.filter(path -> path.getFileName().toString().equals(className + ".java"))
                            .findFirst()
                            .orElse(null);
            return source != null && Files.readString(source).contains("void " + methodName + "(");
        }
    }
}
```

Reading it, one piece at a time:

- **`ReviewChecklistTest`**, a class. Step 6's Done when: every line of the review checklist points at a test that exists. A reference such as ``NPlusOneIT#fetchJoinKeepsQueryCountFlatAt200Orders`` must name a real class and a real method.
  - **field `REFERENCE`** of type `Pattern`, starting as `` Pattern.compile("`([A-Za-z0-9]+)#([A-Za-z0-9]+)`") ``
    - Declaration: `` private static final Pattern REFERENCE = Pattern.compile("`([A-Za-z0-9]+)#([A-Za-z0-9]+)`") ``
  - **field `TESTS`** of type `Path`, starting as `Path.of("src/test/java")`
    - Declaration: `private static final Path TESTS = Path.of("src/test/java")`
  - **test `theChecklistHasFiveLinesAndEveryLineNamesAnExistingTest`**: reads as "the checklist has five lines and every line names an existing test"
    - Signature: `void theChecklistHasFiveLinesAndEveryLineNamesAnExistingTest() throws IOException`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `lines` (of type `List<String>`) holding `Files.readAllLines(Path.of("docs/REVIEW-CHECKLIST.md")).stream() .filter(line -> line.matches("^\\d\\. .*")) .toList()`
      - checks that `lines` has the size `5`
      - creates the variable `missing` (of type `List<String>`) holding `new ArrayList<>()`
      - repeats for `String line : lines`:
        - creates the variable `matcher` (of type `Matcher`) holding `REFERENCE.matcher(line)`
        - creates the variable `found` (of type `boolean`) holding `false`
        - repeats while `matcher.find()`:
          - sets `found` to `true`
          - when `!testMethodExists(matcher.group(1), matcher.group(2))` is true:
            - calls `missing.add(matcher.group(1) + "#" + matcher.group(2))`
        - checks that `found` as `"line names a test: %s", line`
        - and that it is true
      - checks that `missing` as `"referenced tests that do not exist"`
      - and that it is empty
  - **method `testMethodExists`**: the signature is `static boolean testMethodExists(String className, String methodName) throws IOException`.
    - Signature: `private static boolean testMethodExists(String className, String methodName) throws IOException`
    - What the body does, in order:
      - tries this (managing `Stream<Path> files = Files.walk(TESTS)` so it is closed afterwards):
        - creates the variable `source` (of type `Path`) holding `files.filter(path -> path.getFileName().toString().equals(className + ".java")) .findFirst() .orElse(null)`
        - returns `source != null && Files.readString(source).contains("void " + methodName + "(")`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **throws**: `throws X` in a signature says the method may fail with exception `X`; an exception is an object that stops normal flow and travels up the call stack until something catches it.
- **null**: `null` means "no object". Calling a method on null throws `NullPointerException`.
- **try/catch**: `try { ... } catch (X e) { ... }` runs the first block and, if it throws `X`, runs the second.
- **Stream**: A stream processes a collection step by step: `map` converts each item, `filter` keeps some, `toList` collects the result.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.


### `src/test/java/dev/plainround/orders/SqlMirrorTest.java`

Package `dev.plainround.orders`. The Flyway migrations and the copies under sql/migrations must be the same files, byte for byte.

The whole file, as it is in the repository:

```java
package dev.plainround.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The Flyway migrations and the copies under sql/migrations must be the same files, byte for byte.
 */
class SqlMirrorTest {

    private static final Path FLYWAY = Path.of("src/main/resources/db/migration");
    private static final Path MIRROR = Path.of("sql/migrations");

    @Test
    void sqlMigrationsMirrorTheFlywayMigrations() throws IOException {
        List<String> flywayNames = names(FLYWAY);

        assertThat(flywayNames).isNotEmpty();
        assertThat(names(MIRROR)).isEqualTo(flywayNames);
        for (String name : flywayNames) {
            assertThat(Files.readString(MIRROR.resolve(name)))
                    .as("sql/migrations/%s", name)
                    .isEqualTo(Files.readString(FLYWAY.resolve(name)));
        }
    }

    private static List<String> names(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".sql"))
                    .sorted()
                    .toList();
        }
    }
}
```

Reading it, one piece at a time:

- **`SqlMirrorTest`**, a class. The Flyway migrations and the copies under sql/migrations must be the same files, byte for byte.
  - **field `FLYWAY`** of type `Path`, starting as `Path.of("src/main/resources/db/migration")`
    - Declaration: `private static final Path FLYWAY = Path.of("src/main/resources/db/migration")`
  - **field `MIRROR`** of type `Path`, starting as `Path.of("sql/migrations")`
    - Declaration: `private static final Path MIRROR = Path.of("sql/migrations")`
  - **test `sqlMigrationsMirrorTheFlywayMigrations`**: reads as "sql migrations mirror the flyway migrations"
    - Signature: `void sqlMigrationsMirrorTheFlywayMigrations() throws IOException`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `flywayNames` (of type `List<String>`) holding `names(FLYWAY)`
      - checks that `flywayNames` is not empty
      - checks that `names(MIRROR)` equals `flywayNames`
      - repeats for `String name : flywayNames`:
        - checks that `Files.readString(MIRROR.resolve(name))` as `"sql/migrations/%s", name`
        - and that it equals `Files.readString(FLYWAY.resolve(name))`
  - **method `names`**: the signature is `static List<String> names(Path directory) throws IOException`.
    - Signature: `private static List<String> names(Path directory) throws IOException`
    - What the body does, in order:
      - tries this (managing `Stream<Path> files = Files.list(directory)` so it is closed afterwards):
        - returns `files.map(path -> path.getFileName().toString()) .filter(name -> name.endsWith(".sql")) .sorted() .toList()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **throws**: `throws X` in a signature says the method may fail with exception `X`; an exception is an object that stops normal flow and travels up the call stack until something catches it.
- **try/catch**: `try { ... } catch (X e) { ... }` runs the first block and, if it throws `X`, runs the second.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.


### `src/test/java/dev/plainround/orders/application/CheckoutServiceTest.java`

Package `dev.plainround.orders.application`. 

The whole file, as it is in the repository:

```java
package dev.plainround.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.plainround.orders.domain.AuditLog;
import dev.plainround.orders.domain.Customer;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.InvalidOrder;
import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderDraft;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OrderRepository;
import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class CheckoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");
    private static final List<OrderLine> LINES =
            List.of(new OrderLine("sku-1", 2), new OrderLine("sku-2", 1));

    private final CustomerService customers = mock(CustomerService.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final StockRepository stock = mock(StockRepository.class);
    private final AuditLog audit = mock(AuditLog.class);
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service =
                new CheckoutService(
                        customers, orders, stock, audit, Clock.fixed(NOW, ZoneOffset.UTC));
        when(customers.require(1)).thenReturn(new Customer(1, "Ada"));
        when(orders.place(any(OrderDraft.class), any(Instant.class)))
                .thenReturn(new Order(10, 1, NOW, LINES));
    }

    @Test
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

    @Test
    void anOutOfStockLineStopsTheCheckoutAndLaterLinesAreNotReserved() {
        doThrow(new OutOfStock("sku-1")).when(stock).reserve("sku-1", 2);

        assertThatThrownBy(() -> service.checkout(1, LINES)).isInstanceOf(OutOfStock.class);

        verify(stock, never()).reserve("sku-2", 1);
    }

    @Test
    void anUnknownCustomerStillLeavesTheAuditEntryAndPlacesNothing() {
        when(customers.require(9)).thenThrow(new CustomerNotFound(9));

        assertThatThrownBy(() -> service.checkout(9, LINES)).isInstanceOf(CustomerNotFound.class);

        verify(audit).record("CHECKOUT_ATTEMPT", "customer 9, 2 line(s)");
        verify(orders, never()).place(any(), any());
        verify(stock, never()).reserve(anyString(), anyInt());
    }

    @Test
    void aRepeatedSkuIsRejectedBeforeAnythingIsStored() {
        List<OrderLine> repeated = List.of(new OrderLine("sku-1", 1), new OrderLine("sku-1", 2));

        assertThatThrownBy(() -> service.checkout(1, repeated)).isInstanceOf(InvalidOrder.class);

        verify(orders, never()).place(any(), any());
    }
}
```

Reading it, one piece at a time:

- **`CheckoutServiceTest`**, a class.
  - **field `NOW`** of type `Instant`, starting as `Instant.parse("2026-10-27T10:00:00Z")`
    - Declaration: `private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z")`
  - **field `LINES`** of type `List<OrderLine>`, starting as `List.of(new OrderLine("sku-1", 2), new OrderLine("sku-2", 1))`
    - Declaration: `private static final List<OrderLine> LINES = List.of(new OrderLine("sku-1", 2), new OrderLine("sku-2", 1))`
  - **field `customers`** of type `CustomerService`, starting as `mock(CustomerService.class)`
    - Declaration: `private final CustomerService customers = mock(CustomerService.class)`
  - **field `orders`** of type `OrderRepository`, starting as `mock(OrderRepository.class)`
    - Declaration: `private final OrderRepository orders = mock(OrderRepository.class)`
  - **field `stock`** of type `StockRepository`, starting as `mock(StockRepository.class)`
    - Declaration: `private final StockRepository stock = mock(StockRepository.class)`
  - **field `audit`** of type `AuditLog`, starting as `mock(AuditLog.class)`
    - Declaration: `private final AuditLog audit = mock(AuditLog.class)`
  - **field `service`** of type `CheckoutService`, set once when the object is built.
    - Declaration: `private CheckoutService service`
  - **method `setUp`**: the signature is `void setUp()`.
    - Signature: `void setUp()`
    - annotation `@BeforeEach`: runs this method before every test, to prepare a known starting state
    - What the body does, in order:
      - sets `service` to `new CheckoutService( customers, orders, stock, audit, Clock.fixed(NOW, ZoneOffset.UTC))`
      - teaches the fake: when `customers.require(1)` is called, it answers with `new Customer(1, "Ada")`
      - calls `when(orders.place(any(OrderDraft.class), any(Instant.class))) .thenReturn(new Order(10, 1, NOW, LINES))`
  - **test `checkoutAuditsFirstThenPlacesTheOrderThenReservesEveryLine`**: reads as "checkout audits first then places the order then reserves every line"
    - Signature: `void checkoutAuditsFirstThenPlacesTheOrderThenReservesEveryLine()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `placed` (of type `Order`) holding `service.checkout(1, LINES)`
      - checks that `placed.id()` equals `10`
      - creates the variable `inOrder` (of type `InOrder`) holding `inOrder(audit, customers, orders, stock)`
      - calls `inOrder.verify(audit).record("CHECKOUT_ATTEMPT", "customer 1, 2 line(s)")`
      - calls `inOrder.verify(customers).require(1)`
      - calls `inOrder.verify(orders).place(new OrderDraft(1, LINES), NOW)`
      - calls `inOrder.verify(stock).reserve("sku-1", 2)`
      - calls `inOrder.verify(stock).reserve("sku-2", 1)`
  - **test `anOutOfStockLineStopsTheCheckoutAndLaterLinesAreNotReserved`**: reads as "an out of stock line stops the checkout and later lines are not reserved"
    - Signature: `void anOutOfStockLineStopsTheCheckoutAndLaterLinesAreNotReserved()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - calls `doThrow(new OutOfStock("sku-1")).when(stock).reserve("sku-1", 2)`
      - checks that running `service.checkout(1, LINES)` fails
        - the failure is an instance of `OutOfStock.class`
      - checks, with Mockito, that the fake was used as follows: `verify(stock, never()).reserve("sku-2", 1)`
  - **test `anUnknownCustomerStillLeavesTheAuditEntryAndPlacesNothing`**: reads as "an unknown customer still leaves the audit entry and places nothing"
    - Signature: `void anUnknownCustomerStillLeavesTheAuditEntryAndPlacesNothing()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - teaches the fake: when `customers.require(9)` is called, it fails with `new CustomerNotFound(9)`
      - checks that running `service.checkout(9, LINES)` fails
        - the failure is an instance of `CustomerNotFound.class`
      - checks, with Mockito, that the fake was used as follows: `verify(audit).record("CHECKOUT_ATTEMPT", "customer 9, 2 line(s)")`
      - checks, with Mockito, that the fake was used as follows: `verify(orders, never()).place(any(), any())`
      - checks, with Mockito, that the fake was used as follows: `verify(stock, never()).reserve(anyString(), anyInt())`
  - **test `aRepeatedSkuIsRejectedBeforeAnythingIsStored`**: reads as "a repeated sku is rejected before anything is stored"
    - Signature: `void aRepeatedSkuIsRejectedBeforeAnythingIsStored()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `repeated` (of type `List<OrderLine>`) holding `List.of(new OrderLine("sku-1", 1), new OrderLine("sku-1", 2))`
      - checks that running `service.checkout(1, repeated)` fails
        - the failure is an instance of `InvalidOrder.class`
      - checks, with Mockito, that the fake was used as follows: `verify(orders, never()).place(any(), any())`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@BeforeEach`: runs this method before every test, to prepare a known starting state.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.
- **`assertThatThrownBy`**: `assertThatThrownBy(() -> code)` runs the code and checks that it throws; the chained calls describe the exception expected.
- **Mockito stubbing**: `given(mock.call()).willReturn(x)` (or `when(...).thenReturn(x)`) teaches a Mockito fake what to answer.
- **Mockito `verify`**: `verify(mock).call()` checks that the fake was called that way.
- **Mockito `mock`**: `mock(Type.class)` creates a fake object of that type that records the calls made to it.


### `src/test/java/dev/plainround/orders/application/QueryAndCustomerServicesTest.java`

Package `dev.plainround.orders.application`. 

The whole file, as it is in the repository:

```java
package dev.plainround.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.plainround.orders.domain.AuditEntry;
import dev.plainround.orders.domain.AuditLog;
import dev.plainround.orders.domain.Customer;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.CustomerRepository;
import dev.plainround.orders.domain.InvalidCustomer;
import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OrderRepository;
import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QueryAndCustomerServicesTest {

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final CustomerService customers = new CustomerService(customerRepository);

    @Test
    void registerStoresAValidatedName() {
        when(customerRepository.register("Ada")).thenReturn(new Customer(1, "Ada"));

        assertThat(customers.register("Ada").id()).isEqualTo(1);
    }

    @Test
    void registerRejectsABlankNameBeforeStoring() {
        assertThatThrownBy(() -> customers.register(" ")).isInstanceOf(InvalidCustomer.class);

        verify(customerRepository, never()).register(" ");
    }

    @Test
    void requireOfAnUnknownIdIsCustomerNotFound() {
        when(customerRepository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customers.require(5)).isInstanceOf(CustomerNotFound.class);
    }

    @Test
    void listOrdersChecksTheCustomerThenReadsTheOrders() {
        OrderRepository orders = mock(OrderRepository.class);
        when(customerRepository.findById(1)).thenReturn(Optional.of(new Customer(1, "Ada")));
        Order order =
                new Order(
                        10,
                        1,
                        Instant.parse("2026-10-27T10:00:00Z"),
                        List.of(new OrderLine("sku-1", 1)));
        when(orders.findByCustomerId(1)).thenReturn(List.of(order));

        assertThat(new OrderQueryService(customers, orders).listOrders(1)).containsExactly(order);
    }

    @Test
    void stockServiceSetsAndReadsLevelsAndNamesAMissingSku() {
        StockRepository stock = mock(StockRepository.class);
        StockService service = new StockService(stock);
        when(stock.available("sku-1")).thenReturn(Optional.of(4));
        when(stock.available("nope")).thenReturn(Optional.empty());

        service.setAvailable("sku-1", 4);

        verify(stock).setAvailable("sku-1", 4);
        assertThat(service.available("sku-1")).isEqualTo(4);
        assertThatThrownBy(() -> service.available("nope")).isInstanceOf(OutOfStock.class);
    }

    @Test
    void auditServiceReturnsTheNewestEntries() {
        AuditLog log = mock(AuditLog.class);
        AuditEntry entry =
                new AuditEntry(
                        1, "CHECKOUT_ATTEMPT", "customer 1", Instant.parse("2026-10-27T10:00:00Z"));
        when(log.latest(5)).thenReturn(List.of(entry));

        assertThat(new AuditService(log).latest(5)).containsExactly(entry);
    }
}
```

Reading it, one piece at a time:

- **`QueryAndCustomerServicesTest`**, a class.
  - **field `customerRepository`** of type `CustomerRepository`, starting as `mock(CustomerRepository.class)`
    - Declaration: `private final CustomerRepository customerRepository = mock(CustomerRepository.class)`
  - **field `customers`** of type `CustomerService`, starting as `new CustomerService(customerRepository)`
    - Declaration: `private final CustomerService customers = new CustomerService(customerRepository)`
  - **test `registerStoresAValidatedName`**: reads as "register stores a validated name"
    - Signature: `void registerStoresAValidatedName()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - teaches the fake: when `customerRepository.register("Ada")` is called, it answers with `new Customer(1, "Ada")`
      - checks that `customers.register("Ada").id()` equals `1`
  - **test `registerRejectsABlankNameBeforeStoring`**: reads as "register rejects a blank name before storing"
    - Signature: `void registerRejectsABlankNameBeforeStoring()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - checks that running `customers.register(" ")` fails
        - the failure is an instance of `InvalidCustomer.class`
      - checks, with Mockito, that the fake was used as follows: `verify(customerRepository, never()).register(" ")`
  - **test `requireOfAnUnknownIdIsCustomerNotFound`**: reads as "require of an unknown id is customer not found"
    - Signature: `void requireOfAnUnknownIdIsCustomerNotFound()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - teaches the fake: when `customerRepository.findById(5)` is called, it answers with `Optional.empty()`
      - checks that running `customers.require(5)` fails
        - the failure is an instance of `CustomerNotFound.class`
  - **test `listOrdersChecksTheCustomerThenReadsTheOrders`**: reads as "list orders checks the customer then reads the orders"
    - Signature: `void listOrdersChecksTheCustomerThenReadsTheOrders()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `orders` (of type `OrderRepository`) holding `mock(OrderRepository.class)`
      - teaches the fake: when `customerRepository.findById(1)` is called, it answers with `Optional.of(new Customer(1, "Ada"))`
      - creates the variable `order` (of type `Order`) holding `new Order( 10, 1, Instant.parse("2026-10-27T10:00:00Z"), List.of(new OrderLine("sku-1", 1)))`
      - teaches the fake: when `orders.findByCustomerId(1)` is called, it answers with `List.of(order)`
      - checks that `new OrderQueryService(customers, orders).listOrders(1)` contains exactly, in order, `order`
  - **test `stockServiceSetsAndReadsLevelsAndNamesAMissingSku`**: reads as "stock service sets and reads levels and names a missing sku"
    - Signature: `void stockServiceSetsAndReadsLevelsAndNamesAMissingSku()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `stock` (of type `StockRepository`) holding `mock(StockRepository.class)`
      - creates the variable `service` (of type `StockService`) holding `new StockService(stock)`
      - teaches the fake: when `stock.available("sku-1")` is called, it answers with `Optional.of(4)`
      - teaches the fake: when `stock.available("nope")` is called, it answers with `Optional.empty()`
      - calls `service.setAvailable("sku-1", 4)`
      - checks, with Mockito, that the fake was used as follows: `verify(stock).setAvailable("sku-1", 4)`
      - checks that `service.available("sku-1")` equals `4`
      - checks that running `service.available("nope")` fails
        - the failure is an instance of `OutOfStock.class`
  - **test `auditServiceReturnsTheNewestEntries`**: reads as "audit service returns the newest entries"
    - Signature: `void auditServiceReturnsTheNewestEntries()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `log` (of type `AuditLog`) holding `mock(AuditLog.class)`
      - creates the variable `entry` (of type `AuditEntry`) holding `new AuditEntry( 1, "CHECKOUT_ATTEMPT", "customer 1", Instant.parse("2026-10-27T10:00:00Z"))`
      - teaches the fake: when `log.latest(5)` is called, it answers with `List.of(entry)`
      - checks that `new AuditService(log).latest(5)` contains exactly, in order, `entry`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.
- **`assertThatThrownBy`**: `assertThatThrownBy(() -> code)` runs the code and checks that it throws; the chained calls describe the exception expected.
- **Mockito stubbing**: `given(mock.call()).willReturn(x)` (or `when(...).thenReturn(x)`) teaches a Mockito fake what to answer.
- **Mockito `verify`**: `verify(mock).call()` checks that the fake was called that way.
- **Mockito `mock`**: `mock(Type.class)` creates a fake object of that type that records the calls made to it.


### `src/test/java/dev/plainround/orders/domain/CustomerAndOrderTest.java`

Package `dev.plainround.orders.domain`. 

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustomerAndOrderTest {

    @Test
    void aCustomerNeedsANameOfOneToHundredCharacters() {
        assertThat(new Customer(1, "Ada").name()).isEqualTo("Ada");
        assertThatThrownBy(() -> new Customer(1, " ")).isInstanceOf(InvalidCustomer.class);
        assertThatThrownBy(() -> Customer.requireName(null)).isInstanceOf(InvalidCustomer.class);
        assertThatThrownBy(() -> Customer.requireName("x".repeat(101)))
                .isInstanceOf(InvalidCustomer.class);
    }

    @Test
    void aStoredOrderKeepsItsOwnCopyOfTheLines() {
        List<OrderLine> lines = new ArrayList<>(List.of(new OrderLine("sku-1", 1)));
        Order order = new Order(7, 1, Instant.parse("2026-10-27T10:00:00Z"), lines);

        lines.clear();

        assertThat(order.lines()).containsExactly(new OrderLine("sku-1", 1));
    }

    @Test
    void namedErrorsSayWhatWentWrong() {
        assertThat(new CustomerNotFound(9)).hasMessage("no customer with id 9");
        assertThat(new OutOfStock("sku-1")).hasMessage("not enough stock for sku-1");
        assertThat(new StockConflict("sku-1")).hasMessageContaining("another checkout");
    }
}
```

Reading it, one piece at a time:

- **`CustomerAndOrderTest`**, a class.
  - **test `aCustomerNeedsANameOfOneToHundredCharacters`**: reads as "a customer needs a name of one to hundred characters"
    - Signature: `void aCustomerNeedsANameOfOneToHundredCharacters()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - checks that `new Customer(1, "Ada").name()` equals `"Ada"`
      - checks that running `new Customer(1, " ")` fails
        - the failure is an instance of `InvalidCustomer.class`
      - checks that running `Customer.requireName(null)` fails
        - the failure is an instance of `InvalidCustomer.class`
      - checks that running `Customer.requireName("x".repeat(101))` fails
        - the failure is an instance of `InvalidCustomer.class`
  - **test `aStoredOrderKeepsItsOwnCopyOfTheLines`**: reads as "a stored order keeps its own copy of the lines"
    - Signature: `void aStoredOrderKeepsItsOwnCopyOfTheLines()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `lines` (of type `List<OrderLine>`) holding `new ArrayList<>(List.of(new OrderLine("sku-1", 1)))`
      - creates the variable `order` (of type `Order`) holding `new Order(7, 1, Instant.parse("2026-10-27T10:00:00Z"), lines)`
      - calls `lines.clear()`
      - checks that `order.lines()` contains exactly, in order, `new OrderLine("sku-1", 1)`
  - **test `namedErrorsSayWhatWentWrong`**: reads as "named errors say what went wrong"
    - Signature: `void namedErrorsSayWhatWentWrong()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - checks that `new CustomerNotFound(9)` has the message `"no customer with id 9"`
      - checks that `new OutOfStock("sku-1")` has the message `"not enough stock for sku-1"`
      - checks that `new StockConflict("sku-1")` has a message containing `"another checkout"`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **null**: `null` means "no object". Calling a method on null throws `NullPointerException`.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.
- **`assertThatThrownBy`**: `assertThatThrownBy(() -> code)` runs the code and checks that it throws; the chained calls describe the exception expected.


### `src/test/java/dev/plainround/orders/domain/OrderDraftTest.java`

Package `dev.plainround.orders.domain`. 

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class OrderDraftTest {

    @Test
    void aDraftNeedsAtLeastOneLine() {
        assertThatThrownBy(() -> new OrderDraft(1, List.of()))
                .isInstanceOf(InvalidOrder.class)
                .hasMessageContaining("1 to 50 lines");
    }

    @Test
    void aDraftHasAtMostFiftyLines() {
        List<OrderLine> fiftyOne =
                IntStream.rangeClosed(1, 51)
                        .mapToObj(index -> new OrderLine("sku-" + index, 1))
                        .toList();

        assertThatThrownBy(() -> new OrderDraft(1, fiftyOne)).isInstanceOf(InvalidOrder.class);
    }

    @Test
    void theSameSkuOnTwoLinesIsRejected() {
        assertThatThrownBy(
                        () ->
                                new OrderDraft(
                                        1,
                                        List.of(
                                                new OrderLine("sku-1", 1),
                                                new OrderLine("sku-1", 2))))
                .isInstanceOf(InvalidOrder.class)
                .hasMessageContaining("sku");
    }

    @Test
    void theDraftKeepsItsOwnCopyOfTheLines() {
        List<OrderLine> lines = new ArrayList<>(List.of(new OrderLine("sku-1", 1)));
        OrderDraft draft = new OrderDraft(1, lines);

        lines.add(new OrderLine("sku-2", 1));

        assertThat(draft.lines()).hasSize(1);
    }
}
```

Reading it, one piece at a time:

- **`OrderDraftTest`**, a class.
  - **test `aDraftNeedsAtLeastOneLine`**: reads as "a draft needs at least one line"
    - Signature: `void aDraftNeedsAtLeastOneLine()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - checks that running `new OrderDraft(1, List.of())` fails
        - the failure is an instance of `InvalidOrder.class`
        - the failure has a message containing `"1 to 50 lines"`
  - **test `aDraftHasAtMostFiftyLines`**: reads as "a draft has at most fifty lines"
    - Signature: `void aDraftHasAtMostFiftyLines()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `fiftyOne` (of type `List<OrderLine>`) holding `IntStream.rangeClosed(1, 51) .mapToObj(index -> new OrderLine("sku-" + index, 1)) .toList()`
      - checks that running `new OrderDraft(1, fiftyOne)` fails
        - the failure is an instance of `InvalidOrder.class`
  - **test `theSameSkuOnTwoLinesIsRejected`**: reads as "the same sku on two lines is rejected"
    - Signature: `void theSameSkuOnTwoLinesIsRejected()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - checks that running `new OrderDraft( 1, List.of( new OrderLine("sku-1", 1), new OrderLine("sku-1", 2)))` fails
        - the failure is an instance of `InvalidOrder.class`
        - the failure has a message containing `"sku"`
  - **test `theDraftKeepsItsOwnCopyOfTheLines`**: reads as "the draft keeps its own copy of the lines"
    - Signature: `void theDraftKeepsItsOwnCopyOfTheLines()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `lines` (of type `List<OrderLine>`) holding `new ArrayList<>(List.of(new OrderLine("sku-1", 1)))`
      - creates the variable `draft` (of type `OrderDraft`) holding `new OrderDraft(1, lines)`
      - calls `lines.add(new OrderLine("sku-2", 1))`
      - checks that `draft.lines()` has the size `1`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.
- **`assertThatThrownBy`**: `assertThatThrownBy(() -> code)` runs the code and checks that it throws; the chained calls describe the exception expected.


### `src/test/java/dev/plainround/orders/domain/OrderLineTest.java`

Package `dev.plainround.orders.domain`. 

The whole file, as it is in the repository:

```java
package dev.plainround.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderLineTest {

    @Test
    void aLineKeepsItsSkuAndQuantity() {
        OrderLine line = new OrderLine("sku-1", 3);

        assertThat(line.sku()).isEqualTo("sku-1");
        assertThat(line.quantity()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 1001})
    void quantitiesOutsideOneToAThousandAreRejected(int quantity) {
        assertThatThrownBy(() -> new OrderLine("sku-1", quantity))
                .isInstanceOf(InvalidOrder.class)
                .hasMessageContaining("quantity");
    }

    @Test
    void blankOrOverlongSkusAreRejected() {
        assertThatThrownBy(() -> new OrderLine(" ", 1)).isInstanceOf(InvalidOrder.class);
        assertThatThrownBy(() -> new OrderLine(null, 1)).isInstanceOf(InvalidOrder.class);
        assertThatThrownBy(() -> new OrderLine("x".repeat(65), 1)).isInstanceOf(InvalidOrder.class);
    }
}
```

Reading it, one piece at a time:

- **`OrderLineTest`**, a class.
  - **test `aLineKeepsItsSkuAndQuantity`**: reads as "a line keeps its sku and quantity"
    - Signature: `void aLineKeepsItsSkuAndQuantity()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `line` (of type `OrderLine`) holding `new OrderLine("sku-1", 3)`
      - checks that `line.sku()` equals `"sku-1"`
      - checks that `line.quantity()` equals `3`
  - **test `quantitiesOutsideOneToAThousandAreRejected`**: reads as "quantities outside one to a thousand are rejected"
    - Signature: `void quantitiesOutsideOneToAThousandAreRejected(int quantity)`
    - annotation `@ParameterizedTest`: marks a test that runs several times with different inputs
    - annotation `@ValueSource(ints = {0, -1, 1001})`: gives a parameterized test the list of values; the test runs once per value
    - What the body does, in order:
      - checks that running `new OrderLine("sku-1", quantity)` fails
        - the failure is an instance of `InvalidOrder.class`
        - the failure has a message containing `"quantity"`
  - **test `blankOrOverlongSkusAreRejected`**: reads as "blank or overlong skus are rejected"
    - Signature: `void blankOrOverlongSkusAreRejected()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - checks that running `new OrderLine(" ", 1)` fails
        - the failure is an instance of `InvalidOrder.class`
      - checks that running `new OrderLine(null, 1)` fails
        - the failure is an instance of `InvalidOrder.class`
      - checks that running `new OrderLine("x".repeat(65), 1)` fails
        - the failure is an instance of `InvalidOrder.class`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.
- `@ParameterizedTest`: marks a test that runs several times with different inputs.
- `@ValueSource`: gives a parameterized test the list of values; the test runs once per value.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **null**: `null` means "no object". Calling a method on null throws `NullPointerException`.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.
- **`assertThatThrownBy`**: `assertThatThrownBy(() -> code)` runs the code and checks that it throws; the chained calls describe the exception expected.


### `src/test/java/dev/plainround/orders/interfaces/web/CustomerStockAuditControllersTest.java`

Package `dev.plainround.orders.interfaces.web`. 

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.plainround.orders.application.AuditService;
import dev.plainround.orders.application.CustomerService;
import dev.plainround.orders.application.StockService;
import dev.plainround.orders.domain.AuditEntry;
import dev.plainround.orders.domain.Customer;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.OutOfStock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CustomerController.class, StockController.class, AuditController.class})
class CustomerStockAuditControllersTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CustomerService customers;

    @MockitoBean private StockService stock;

    @MockitoBean private AuditService audit;

    @Test
    void registerReturns201WithTheNewCustomer() throws Exception {
        when(customers.register("Ada")).thenReturn(new Customer(1, "Ada"));

        mockMvc.perform(
                        post("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Ada\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void aBlankNameIs400NamingTheField() throws Exception {
        mockMvc.perform(
                        post("/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void getOfAnUnknownCustomerIs404() throws Exception {
        when(customers.require(7)).thenThrow(new CustomerNotFound(7));

        mockMvc.perform(get("/customers/7")).andExpect(status().isNotFound());
    }

    @Test
    void putSetsTheStockLevel() throws Exception {
        mockMvc.perform(
                        put("/stock/sku-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"available\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(5));

        verify(stock).setAvailable("sku-1", 5);
    }

    @Test
    void aNegativeLevelIs400() throws Exception {
        mockMvc.perform(
                        put("/stock/sku-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"available\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("available"));
    }

    @Test
    void getOfASkuWithoutAStockRecordIs409OutOfStock() throws Exception {
        when(stock.available("nope")).thenThrow(new OutOfStock("nope"));

        mockMvc.perform(get("/stock/nope")).andExpect(status().isConflict());
    }

    @Test
    void auditReturnsTheNewestEntriesAndBoundsTheLimit() throws Exception {
        when(audit.latest(20))
                .thenReturn(
                        List.of(
                                new AuditEntry(
                                        1,
                                        "CHECKOUT_ATTEMPT",
                                        "customer 1",
                                        Instant.parse("2026-10-27T10:00:00Z"))));

        mockMvc.perform(get("/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CHECKOUT_ATTEMPT"));
        mockMvc.perform(get("/audit").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
    }
}
```

Reading it, one piece at a time:

- **`CustomerStockAuditControllersTest`**, a class.
  - annotation `@WebMvcTest({CustomerController.class, StockController.class, AuditController.class})`: starts only the web layer (controllers, JSON conversion, validation) for a fast test
  - **field `mockMvc`** of type `MockMvc`, set once when the object is built.
    - Declaration: `private MockMvc mockMvc`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `customers`** of type `CustomerService`, set once when the object is built.
    - Declaration: `private CustomerService customers`
    - annotation `@MockitoBean`: puts a Mockito fake in the Spring context in place of the real bean of that type
  - **field `stock`** of type `StockService`, set once when the object is built.
    - Declaration: `private StockService stock`
    - annotation `@MockitoBean`: puts a Mockito fake in the Spring context in place of the real bean of that type
  - **field `audit`** of type `AuditService`, set once when the object is built.
    - Declaration: `private AuditService audit`
    - annotation `@MockitoBean`: puts a Mockito fake in the Spring context in place of the real bean of that type
  - **test `registerReturns201WithTheNewCustomer`**: reads as "register returns201 with the new customer"
    - Signature: `void registerReturns201WithTheNewCustomer() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - teaches the fake: when `customers.register("Ada")` is called, it answers with `new Customer(1, "Ada")`
        - then `mockMvc`
      - sends the fake HTTP request `post("/customers") .contentType(MediaType.APPLICATION_JSON) .content("{\"name\":\"Ada\"}")`
        - expects `status().isCreated()`
        - expects `jsonPath("$.id").value(1)`
  - **test `aBlankNameIs400NamingTheField`**: reads as "a blank name is400 naming the field"
    - Signature: `void aBlankNameIs400NamingTheField() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
        - then `mockMvc`
      - sends the fake HTTP request `post("/customers") .contentType(MediaType.APPLICATION_JSON) .content("{\"name\":\" \"}")`
        - expects `status().isBadRequest()`
        - expects `jsonPath("$.errors[0].field").value("name")`
  - **test `getOfAnUnknownCustomerIs404`**: reads as "get of an unknown customer is404"
    - Signature: `void getOfAnUnknownCustomerIs404() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - teaches the fake: when `customers.require(7)` is called, it fails with `new CustomerNotFound(7)`
        - then `mockMvc`
      - sends the fake HTTP request `get("/customers/7")`
        - expects `status().isNotFound()`
  - **test `putSetsTheStockLevel`**: reads as "put sets the stock level"
    - Signature: `void putSetsTheStockLevel() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
        - then `mockMvc`
      - sends the fake HTTP request `put("/stock/sku-1") .contentType(MediaType.APPLICATION_JSON) .content("{\"available\":5}")`
        - expects `status().isOk()`
        - expects `jsonPath("$.available").value(5)`
      - checks, with Mockito, that the fake was used as follows: `verify(stock).setAvailable("sku-1", 5)`
  - **test `aNegativeLevelIs400`**: reads as "a negative level is400"
    - Signature: `void aNegativeLevelIs400() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
        - then `mockMvc`
      - sends the fake HTTP request `put("/stock/sku-1") .contentType(MediaType.APPLICATION_JSON) .content("{\"available\":-1}")`
        - expects `status().isBadRequest()`
        - expects `jsonPath("$.errors[0].field").value("available")`
  - **test `getOfASkuWithoutAStockRecordIs409OutOfStock`**: reads as "get of a sku without a stock record is409 out of stock"
    - Signature: `void getOfASkuWithoutAStockRecordIs409OutOfStock() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - teaches the fake: when `stock.available("nope")` is called, it fails with `new OutOfStock("nope")`
        - then `mockMvc`
      - sends the fake HTTP request `get("/stock/nope")`
        - expects `status().isConflict()`
  - **test `auditReturnsTheNewestEntriesAndBoundsTheLimit`**: reads as "audit returns the newest entries and bounds the limit"
    - Signature: `void auditReturnsTheNewestEntriesAndBoundsTheLimit() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - calls `when(audit.latest(20)) .thenReturn( List.of( new AuditEntry( 1, "CHECKOUT_ATTEMPT", "customer 1", Instant.parse("2026-10-27T10:00:00Z"))))`
        - then `mockMvc`
      - sends the fake HTTP request `get("/audit")`
        - expects `status().isOk()`
        - expects `jsonPath("$[0].action").value("CHECKOUT_ATTEMPT")`
        - then `mockMvc`
      - sends the fake HTTP request `get("/audit").param("limit", "101")`
        - expects `status().isBadRequest()`
        - expects `jsonPath("$.errors[0].field").value("limit")`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@WebMvcTest`: starts only the web layer (controllers, JSON conversion, validation) for a fast test.
- `@Autowired`: asks Spring to supply the object this field or parameter needs.
- `@MockitoBean`: puts a Mockito fake in the Spring context in place of the real bean of that type.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **throws**: `throws X` in a signature says the method may fail with exception `X`; an exception is an object that stops normal flow and travels up the call stack until something catches it.
- **MockMvc**: `MockMvc` sends a fake HTTP request straight into the controllers without a network, then lets the test inspect the response.
- **Mockito stubbing**: `given(mock.call()).willReturn(x)` (or `when(...).thenReturn(x)`) teaches a Mockito fake what to answer.
- **Mockito `verify`**: `verify(mock).call()` checks that the fake was called that way.


### `src/test/java/dev/plainround/orders/interfaces/web/OrderControllerTest.java`

Package `dev.plainround.orders.interfaces.web`. The web layer only: the controller, its advice and the MVC machinery; the services are test doubles.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.plainround.orders.application.CheckoutService;
import dev.plainround.orders.application.OrderQueryService;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.InvalidOrder;
import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockConflict;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The web layer only: the controller, its advice and the MVC machinery; the services are test
 * doubles.
 */
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

    @Test
    void checkoutReturns201WithTheStoredOrder() throws Exception {
        when(checkout.checkout(anyLong(), anyList())).thenReturn(STORED);

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CHECKOUT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.lines[0].sku").value("sku-1"));
    }

    @Test
    void anEmptyBasketIs400NamingTheLines() throws Exception {
        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors[0].field").value("lines"));
    }

    @Test
    void aBadLineIs400NamingTheLineAndField() throws Exception {
        String body = "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":0}]}";

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("lines[0].quantity"));
    }

    @Test
    void anUnknownCustomerIs404() throws Exception {
        when(checkout.checkout(anyLong(), anyList())).thenThrow(new CustomerNotFound(1));

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CHECKOUT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"))
                .andExpect(jsonPath("$.detail").value("no customer with id 1"));
    }

    @Test
    void anOutOfStockLineIs409() throws Exception {
        when(checkout.checkout(anyLong(), anyList())).thenThrow(new OutOfStock("sku-1"));

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CHECKOUT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Out of stock"));
    }

    @Test
    void aLostStockRaceIs409NamedConcurrentUpdate() throws Exception {
        when(checkout.checkout(anyLong(), anyList())).thenThrow(new StockConflict("sku-1"));

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CHECKOUT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent update"));
    }

    @Test
    void aRuleTheAnnotationsCannotExpressIs422() throws Exception {
        when(checkout.checkout(anyLong(), anyList()))
                .thenThrow(new InvalidOrder("each sku may appear on one line only"));

        mockMvc.perform(
                        post("/customers/1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CHECKOUT))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("each sku may appear on one line only"));
    }

    @Test
    void listReturnsTheOrdersWithTheirLines() throws Exception {
        when(queries.listOrders(1)).thenReturn(List.of(STORED));

        mockMvc.perform(get("/customers/1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lines[0].quantity").value(2));
    }

    @Test
    void aNonNumericCustomerIdIsAProblemDetail400() throws Exception {
        mockMvc.perform(get("/customers/abc/orders"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void anUnexpectedErrorIs500WithoutLeakingTheMessage() throws Exception {
        when(queries.listOrders(1))
                .thenThrow(new IllegalStateException("jdbc:postgresql://secret-host/orders"));

        mockMvc.perform(get("/customers/1/orders"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal error"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "secret-host"))));
    }
}
```

Reading it, one piece at a time:

- **`OrderControllerTest`**, a class. The web layer only: the controller, its advice and the MVC machinery; the services are test doubles.
  - annotation `@WebMvcTest(OrderController.class)`: starts only the web layer (controllers, JSON conversion, validation) for a fast test
  - **field `CHECKOUT`** of type `String`, starting as `"{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":2}]}"`
    - Declaration: `private static final String CHECKOUT = "{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":2}]}"`
  - **field `STORED`** of type `Order`, starting as `new Order( 10, 1, Instant.parse("2026-10-27T10:00:00Z"), List.of(new OrderLine("sku-1", 2)))`
    - Declaration: `private static final Order STORED = new Order( 10, 1, Instant.parse("2026-10-27T10:00:00Z"), List.of(new OrderLine("sku-1", 2)))`
  - **field `mockMvc`** of type `MockMvc`, set once when the object is built.
    - Declaration: `private MockMvc mockMvc`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `checkout`** of type `CheckoutService`, set once when the object is built.
    - Declaration: `private CheckoutService checkout`
    - annotation `@MockitoBean`: puts a Mockito fake in the Spring context in place of the real bean of that type
  - **field `queries`** of type `OrderQueryService`, set once when the object is built.
    - Declaration: `private OrderQueryService queries`
    - annotation `@MockitoBean`: puts a Mockito fake in the Spring context in place of the real bean of that type
  - **test `checkoutReturns201WithTheStoredOrder`**: reads as "checkout returns201 with the stored order"
    - Signature: `void checkoutReturns201WithTheStoredOrder() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - teaches the fake: when `checkout.checkout(anyLong(), anyList())` is called, it answers with `STORED`
        - then `mockMvc`
      - sends the fake HTTP request `post("/customers/1/orders") .contentType(MediaType.APPLICATION_JSON) .content(CHECKOUT)`
        - expects `status().isCreated()`
        - expects `jsonPath("$.id").value(10)`
        - expects `jsonPath("$.lines[0].sku").value("sku-1")`
  - **test `anEmptyBasketIs400NamingTheLines`**: reads as "an empty basket is400 naming the lines"
    - Signature: `void anEmptyBasketIs400NamingTheLines() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
        - then `mockMvc`
      - sends the fake HTTP request `post("/customers/1/orders") .contentType(MediaType.APPLICATION_JSON) .content("{\"lines\":[]}")`
        - expects `status().isBadRequest()`
        - expects `content().contentTypeCompatibleWith("application/problem+json")`
        - expects `jsonPath("$.errors[0].field").value("lines")`
  - **test `aBadLineIs400NamingTheLineAndField`**: reads as "a bad line is400 naming the line and field"
    - Signature: `void aBadLineIs400NamingTheLineAndField() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `body` (of type `String`) holding `"{\"lines\":[{\"sku\":\"sku-1\",\"quantity\":0}]}"`
        - then `mockMvc`
      - sends the fake HTTP request `post("/customers/1/orders") .contentType(MediaType.APPLICATION_JSON) .content(body)`
        - expects `status().isBadRequest()`
        - expects `jsonPath("$.errors[0].field").value("lines[0].quantity")`
  - **test `anUnknownCustomerIs404`**: reads as "an unknown customer is404"
    - Signature: `void anUnknownCustomerIs404() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - teaches the fake: when `checkout.checkout(anyLong(), anyList())` is called, it fails with `new CustomerNotFound(1)`
        - then `mockMvc`
      - sends the fake HTTP request `post("/customers/1/orders") .contentType(MediaType.APPLICATION_JSON) .content(CHECKOUT)`
        - expects `status().isNotFound()`
        - expects `jsonPath("$.title").value("Customer not found")`
        - expects `jsonPath("$.detail").value("no customer with id 1")`
  - **test `anOutOfStockLineIs409`**: reads as "an out of stock line is409"
    - Signature: `void anOutOfStockLineIs409() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - teaches the fake: when `checkout.checkout(anyLong(), anyList())` is called, it fails with `new OutOfStock("sku-1")`
        - then `mockMvc`
      - sends the fake HTTP request `post("/customers/1/orders") .contentType(MediaType.APPLICATION_JSON) .content(CHECKOUT)`
        - expects `status().isConflict()`
        - expects `jsonPath("$.title").value("Out of stock")`
  - **test `aLostStockRaceIs409NamedConcurrentUpdate`**: reads as "a lost stock race is409 named concurrent update"
    - Signature: `void aLostStockRaceIs409NamedConcurrentUpdate() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - teaches the fake: when `checkout.checkout(anyLong(), anyList())` is called, it fails with `new StockConflict("sku-1")`
        - then `mockMvc`
      - sends the fake HTTP request `post("/customers/1/orders") .contentType(MediaType.APPLICATION_JSON) .content(CHECKOUT)`
        - expects `status().isConflict()`
        - expects `jsonPath("$.title").value("Concurrent update")`
  - **test `aRuleTheAnnotationsCannotExpressIs422`**: reads as "a rule the annotations cannot express is422"
    - Signature: `void aRuleTheAnnotationsCannotExpressIs422() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - calls `when(checkout.checkout(anyLong(), anyList())) .thenThrow(new InvalidOrder("each sku may appear on one line only"))`
        - then `mockMvc`
      - sends the fake HTTP request `post("/customers/1/orders") .contentType(MediaType.APPLICATION_JSON) .content(CHECKOUT)`
        - expects `status().isUnprocessableContent()`
        - expects `jsonPath("$.detail").value("each sku may appear on one line only")`
  - **test `listReturnsTheOrdersWithTheirLines`**: reads as "list returns the orders with their lines"
    - Signature: `void listReturnsTheOrdersWithTheirLines() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - teaches the fake: when `queries.listOrders(1)` is called, it answers with `List.of(STORED)`
        - then `mockMvc`
      - sends the fake HTTP request `get("/customers/1/orders")`
        - expects `status().isOk()`
        - expects `jsonPath("$[0].lines[0].quantity").value(2)`
  - **test `aNonNumericCustomerIdIsAProblemDetail400`**: reads as "a non numeric customer id is a problem detail400"
    - Signature: `void aNonNumericCustomerIdIsAProblemDetail400() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
        - then `mockMvc`
      - sends the fake HTTP request `get("/customers/abc/orders")`
        - expects `status().isBadRequest()`
        - expects `content().contentTypeCompatibleWith("application/problem+json")`
  - **test `anUnexpectedErrorIs500WithoutLeakingTheMessage`**: reads as "an unexpected error is500 without leaking the message"
    - Signature: `void anUnexpectedErrorIs500WithoutLeakingTheMessage() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - calls `when(queries.listOrders(1)) .thenThrow(new IllegalStateException("jdbc:postgresql://secret-host/orders"))`
        - then `mockMvc`
      - sends the fake HTTP request `get("/customers/1/orders")`
        - expects `status().isInternalServerError()`
        - expects `jsonPath("$.title").value("Internal error")`
        - expects `content() .string( org.hamcrest.Matchers.not( org.hamcrest.Matchers.containsString( "secret-host")))`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@WebMvcTest`: starts only the web layer (controllers, JSON conversion, validation) for a fast test.
- `@Autowired`: asks Spring to supply the object this field or parameter needs.
- `@MockitoBean`: puts a Mockito fake in the Spring context in place of the real bean of that type.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **throws**: `throws X` in a signature says the method may fail with exception `X`; an exception is an object that stops normal flow and travels up the call stack until something catches it.
- **MockMvc**: `MockMvc` sends a fake HTTP request straight into the controllers without a network, then lets the test inspect the response.
- **Mockito stubbing**: `given(mock.call()).willReturn(x)` (or `when(...).thenReturn(x)`) teaches a Mockito fake what to answer.


### `src/test/java/dev/plainround/orders/interfaces/web/RequestIdFilterTest.java`

Package `dev.plainround.orders.interfaces.web`. 

The whole file, as it is in the repository:

```java
package dev.plainround.orders.interfaces.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void aSafeIncomingIdIsKeptAndPutInTheLogContextForTheRequestOnly() throws Exception {
        AtomicReference<String> insideTheChain = new AtomicReference<>();
        FilterChain chain = (request, response) -> insideTheChain.set(MDC.get("requestId"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "trace-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(insideTheChain).hasValue("trace-42");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("trace-42");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void anUnsafeIncomingIdIsReplacedSoItCannotForgeLogLines() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "evil\n{\"level\":\"ERROR\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader("X-Request-Id")).matches("[0-9a-f-]{36}");
    }

    @Test
    void aMissingIdIsGenerated() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> {});

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
    }
}
```

Reading it, one piece at a time:

- **`RequestIdFilterTest`**, a class.
  - **field `filter`** of type `RequestIdFilter`, starting as `new RequestIdFilter()`
    - Declaration: `private final RequestIdFilter filter = new RequestIdFilter()`
  - **test `aSafeIncomingIdIsKeptAndPutInTheLogContextForTheRequestOnly`**: reads as "a safe incoming id is kept and put in the log context for the request only"
    - Signature: `void aSafeIncomingIdIsKeptAndPutInTheLogContextForTheRequestOnly() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `insideTheChain` (of type `AtomicReference<String>`) holding `new AtomicReference<>()`
      - creates the variable `chain` (of type `FilterChain`) holding `(request, response) -> insideTheChain.set(MDC.get("requestId"))`
      - creates the variable `request` (of type `MockHttpServletRequest`) holding `new MockHttpServletRequest()`
      - calls `request.addHeader("X-Request-Id", "trace-42")`
      - creates the variable `response` (of type `MockHttpServletResponse`) holding `new MockHttpServletResponse()`
      - calls `filter.doFilter(request, response, chain)`
      - checks that `insideTheChain` has value `"trace-42"`
      - checks that `response.getHeader("X-Request-Id")` equals `"trace-42"`
      - checks that `MDC.get("requestId")` is null
  - **test `anUnsafeIncomingIdIsReplacedSoItCannotForgeLogLines`**: reads as "an unsafe incoming id is replaced so it cannot forge log lines"
    - Signature: `void anUnsafeIncomingIdIsReplacedSoItCannotForgeLogLines() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `request` (of type `MockHttpServletRequest`) holding `new MockHttpServletRequest()`
      - calls `request.addHeader("X-Request-Id", "evil\n{\"level\":\"ERROR\"}")`
      - creates the variable `response` (of type `MockHttpServletResponse`) holding `new MockHttpServletResponse()`
      - calls `filter.doFilter(request, response, (req, res) -> {})`
      - checks that `response.getHeader("X-Request-Id")` matches `"[0-9a-f-]{36}"`
  - **test `aMissingIdIsGenerated`**: reads as "a missing id is generated"
    - Signature: `void aMissingIdIsGenerated() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `response` (of type `MockHttpServletResponse`) holding `new MockHttpServletResponse()`
      - calls `filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> {})`
      - checks that `response.getHeader("X-Request-Id")` is not blank

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **throws**: `throws X` in a signature says the method may fail with exception `X`; an exception is an object that stops normal flow and travels up the call stack until something catches it.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.


### `src/test/java/dev/plainround/orders/support/DatabaseReset.java`

Package `dev.plainround.orders.support`. Empties every table between tests that commit real transactions.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.support;

import org.springframework.jdbc.core.JdbcTemplate;

/** Empties every table between tests that commit real transactions. */
public final class DatabaseReset {

    private DatabaseReset() {}

    /**
     * Truncates all application tables and restarts their identity columns.
     *
     * @param jdbc the template on the test database
     */
    public static void truncate(JdbcTemplate jdbc) {
        jdbc.execute(
                "TRUNCATE order_lines, orders_table, customers, stock_items, audit_records RESTART"
                        + " IDENTITY CASCADE");
    }

    /**
     * Counts the rows of a table, independent of JPA and its caches.
     *
     * @param jdbc the template on the test database
     * @param table the table name (a constant of the test, never user input)
     * @return the row count
     */
    public static long count(JdbcTemplate jdbc, String table) {
        Long count = jdbc.queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0 : count;
    }
}
```

Reading it, one piece at a time:

- **`DatabaseReset`**, a class. Empties every table between tests that commit real transactions.
  - **method `DatabaseReset`**: the signature is `DatabaseReset()`.
    - Signature: `private DatabaseReset()`
  - **method `truncate`**: Truncates all application tables and restarts their identity columns.
    - Signature: `public static void truncate(JdbcTemplate jdbc)`
    - input `jdbc`: the template on the test database
    - What the body does, in order:
      - calls `jdbc.execute( "TRUNCATE order_lines, orders_table, customers, stock_items, audit_records RESTART" + " IDENTITY CASCADE")`
  - **method `count`**: Counts the rows of a table, independent of JPA and its caches.
    - Signature: `public static long count(JdbcTemplate jdbc, String table)`
    - input `jdbc`: the template on the test database
    - input `table`: the table name (a constant of the test, never user input)
    - gives back: the row count
    - What the body does, in order:
      - creates the variable `count` (of type `Long`) holding `jdbc.queryForObject("select count(*) from " + table, Long.class)`
      - returns `count == null ? 0 : count`

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **null**: `null` means "no object". Calling a method on null throws `NullPointerException`.


### `src/test/java/dev/plainround/orders/support/Evidence.java`

Package `dev.plainround.orders.support`. Appends the measured numbers of the traps to `target/evidence.txt`, which `make traps` prints.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Appends the measured numbers of the traps to {@code target/evidence.txt}, which {@code make
 * traps} prints.
 */
public final class Evidence {

    private static final Path FILE = Path.of("target/evidence.txt");

    private Evidence() {}

    /**
     * Records one measured line.
     *
     * @param line the text to append
     */
    public static synchronized void record(String line) {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(
                    FILE,
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
```

Reading it, one piece at a time:

- **`Evidence`**, a class. Appends the measured numbers of the traps to `target/evidence.txt`, which `make traps` prints.
  - **field `FILE`** of type `Path`, starting as `Path.of("target/evidence.txt")`
    - Declaration: `private static final Path FILE = Path.of("target/evidence.txt")`
  - **method `Evidence`**: the signature is `Evidence()`.
    - Signature: `private Evidence()`
  - **method `record`**: Records one measured line.
    - Signature: `public static synchronized void record(String line)`
    - input `line`: the text to append
    - What the body does, in order:
      - tries this:
        - calls `Files.createDirectories(FILE.getParent())`
        - calls `Files.writeString( FILE, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND)`
      - if this fails with `IOException exception`:
        - throws a new `UncheckedIOException` created with `exception`

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.
- **try/catch**: `try { ... } catch (X e) { ... }` runs the first block and, if it throws `X`, runs the second.


### `src/test/java/dev/plainround/orders/support/PostgresContainer.java`

Package `dev.plainround.orders.support`. The real database for every integration test: PostgreSQL 18, the same image as docker-compose.yml, started once per test JVM and wired into Spring Boot as the datasource through `@ServiceConnection`.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.support;

import java.time.Duration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The real database for every integration test: PostgreSQL 18, the same image as
 * docker-compose.yml, started once per test JVM and wired into Spring Boot as the datasource
 * through {@code @ServiceConnection}.
 */
public final class PostgresContainer {

    /**
     * The shared container; import it with {@code @ImportTestcontainers(PostgresContainer.class)}.
     */
    @Container @ServiceConnection
    public static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18")
                    .waitingFor(
                            Wait.forLogMessage(
                                            ".*database system is ready to accept connections.*\\s",
                                            2)
                                    .withStartupTimeout(Duration.ofMinutes(5)));

    private PostgresContainer() {}
}
```

Reading it, one piece at a time:

- **`PostgresContainer`**, a class. The real database for every integration test: PostgreSQL 18, the same image as docker-compose.yml, started once per test JVM and wired into Spring Boot as the datasource through `@ServiceConnection`.
  - **field `POSTGRES`** of type `PostgreSQLContainer`: The shared container; import it with `@ImportTestcontainers(PostgresContainer.class)`.
    - Declaration: `public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18") .waitingFor( Wait.forLogMessage( ".*database system is ready to accept connections.*\\s", 2) .withStartupTimeout(Duration.ofMinutes(5)))`
    - annotation `@Container`: Testcontainers: the container in this field is started and stopped by the test class
    - annotation `@ServiceConnection`: Spring Boot: connects the application to this container by reading its address and credentials
  - **method `PostgresContainer`**: the signature is `PostgresContainer()`.
    - Signature: `private PostgresContainer()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Container`: Testcontainers: the container in this field is started and stopped by the test class.
- `@ServiceConnection`: Spring Boot: connects the application to this container by reading its address and credentials.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.


### `src/test/java/dev/plainround/orders/traps/AuditIT.java`

Package `dev.plainround.orders.traps`. Trap 3: an audit record that disappears whenever the checkout it records fails.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.plainround.orders.support.DatabaseReset;
import dev.plainround.orders.support.Evidence;
import dev.plainround.orders.support.PostgresContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Trap 3: an audit record that disappears whenever the checkout it records fails. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportTestcontainers(PostgresContainer.class)
@Import(AuditTrapBeans.class)
class AuditIT {

    @Autowired private AuditTrapBeans.SameTransactionAudit sameTx;
    @Autowired private AuditTrapBeans.RequiresNewAudit requiresNew;
    @Autowired private AuditTrapBeans.SelfInvokedRequiresNewAudit selfInvoked;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void sameTransactionAuditIsLostWhenCheckoutFails() {
        long before = DatabaseReset.count(jdbc, "audit_records");

        assertThatThrownBy(() -> sameTx.checkoutAuditSameTx(true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(before).isZero();
        assertThat(DatabaseReset.count(jdbc, "audit_records")).isZero();
        Evidence.record(
                "audit rows before=0 after (same-tx, checkout failed)="
                        + DatabaseReset.count(jdbc, "audit_records"));
    }

    @Test
    void auditWithRequiresNewSurvivesRollback() {
        assertThatThrownBy(() -> requiresNew.checkoutAuditRequiresNew(true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(DatabaseReset.count(jdbc, "audit_records")).isEqualTo(1);
        Evidence.record(
                "audit rows before=0 after (REQUIRES_NEW, checkout failed)="
                        + DatabaseReset.count(jdbc, "audit_records"));
    }

    @Test
    void requiresNewOnTheSameBeanIsIgnored() {
        assertThatThrownBy(() -> selfInvoked.checkoutAuditSelfInvoked(true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(DatabaseReset.count(jdbc, "audit_records")).isZero();
        Evidence.record(
                "audit rows after REQUIRES_NEW on the same bean, checkout failed="
                        + DatabaseReset.count(jdbc, "audit_records")
                        + "   (annotation ignored)");
    }
}
```

Reading it, one piece at a time:

- **`AuditIT`**, a class. Trap 3: an audit record that disappears whenever the checkout it records fails.
  - annotation `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)`: starts the whole application inside the test (all beans, the real configuration) so the test can use it
  - annotation `@ImportTestcontainers(PostgresContainer.class)`: starts the containers declared in the named class for this test and wires them into the Spring configuration
  - annotation `@Import(AuditTrapBeans.class)`: adds the named configuration classes to the Spring context
  - **field `sameTx`** of type `AuditTrapBeans.SameTransactionAudit`, set once when the object is built.
    - Declaration: `private AuditTrapBeans.SameTransactionAudit sameTx`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `requiresNew`** of type `AuditTrapBeans.RequiresNewAudit`, set once when the object is built.
    - Declaration: `private AuditTrapBeans.RequiresNewAudit requiresNew`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `selfInvoked`** of type `AuditTrapBeans.SelfInvokedRequiresNewAudit`, set once when the object is built.
    - Declaration: `private AuditTrapBeans.SelfInvokedRequiresNewAudit selfInvoked`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `jdbc`** of type `JdbcTemplate`, set once when the object is built.
    - Declaration: `private JdbcTemplate jdbc`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **method `clean`**: the signature is `void clean()`.
    - Signature: `void clean()`
    - annotation `@AfterEach`: runs this method after every test, to clean up
    - What the body does, in order:
      - calls `DatabaseReset.truncate(jdbc)`
  - **test `sameTransactionAuditIsLostWhenCheckoutFails`**: reads as "same transaction audit is lost when checkout fails"
    - Signature: `void sameTransactionAuditIsLostWhenCheckoutFails()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `before` (of type `long`) holding `DatabaseReset.count(jdbc, "audit_records")`
      - checks that running `sameTx.checkoutAuditSameTx(true)` fails
        - the failure is an instance of `IllegalStateException.class`
      - checks that `before` is zero
      - checks that `DatabaseReset.count(jdbc, "audit_records")` is zero
      - calls `Evidence.record( "audit rows before=0 after (same-tx, checkout failed)=" + DatabaseReset.count(jdbc, "audit_records"))`
  - **test `auditWithRequiresNewSurvivesRollback`**: reads as "audit with requires new survives rollback"
    - Signature: `void auditWithRequiresNewSurvivesRollback()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - checks that running `requiresNew.checkoutAuditRequiresNew(true)` fails
        - the failure is an instance of `IllegalStateException.class`
      - checks that `DatabaseReset.count(jdbc, "audit_records")` equals `1`
      - calls `Evidence.record( "audit rows before=0 after (REQUIRES_NEW, checkout failed)=" + DatabaseReset.count(jdbc, "audit_records"))`
  - **test `requiresNewOnTheSameBeanIsIgnored`**: reads as "requires new on the same bean is ignored"
    - Signature: `void requiresNewOnTheSameBeanIsIgnored()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - checks that running `selfInvoked.checkoutAuditSelfInvoked(true)` fails
        - the failure is an instance of `IllegalStateException.class`
      - checks that `DatabaseReset.count(jdbc, "audit_records")` is zero
      - calls `Evidence.record( "audit rows after REQUIRES_NEW on the same bean, checkout failed=" + DatabaseReset.count(jdbc, "audit_records") + " (annotation ignored)")`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@SpringBootTest`: starts the whole application inside the test (all beans, the real configuration) so the test can use it.
- `@ImportTestcontainers`: starts the containers declared in the named class for this test and wires them into the Spring configuration.
- `@Import`: adds the named configuration classes to the Spring context.
- `@Autowired`: asks Spring to supply the object this field or parameter needs.
- `@AfterEach`: runs this method after every test, to clean up.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.
- **`assertThatThrownBy`**: `assertThatThrownBy(() -> code)` runs the code and checks that it throws; the chained calls describe the exception expected.


### `src/test/java/dev/plainround/orders/traps/AuditTrapBeans.java`

Package `dev.plainround.orders.traps`. Trap 3, planted three ways.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import dev.plainround.orders.domain.AuditLog;
import dev.plainround.orders.infrastructure.persistence.AuditJpaRepository;
import dev.plainround.orders.infrastructure.persistence.AuditRecordEntity;
import java.time.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trap 3, planted three ways. Test code only; each variant fails a checkout after it wrote the
 * audit entry.
 */
@TestConfiguration(proxyBeanMethods = false)
class AuditTrapBeans {

    @Bean
    SameTransactionAudit sameTransactionAudit(AuditJpaRepository audits, Clock clock) {
        return new SameTransactionAudit(audits, clock);
    }

    @Bean
    RequiresNewAudit requiresNewAudit(AuditLog auditLog) {
        return new RequiresNewAudit(auditLog);
    }

    @Bean
    SelfInvokedRequiresNewAudit selfInvokedRequiresNewAudit(
            AuditJpaRepository audits, Clock clock) {
        return new SelfInvokedRequiresNewAudit(audits, clock);
    }

    /** THE BUG: the audit entry joins the checkout transaction and rolls back with it. */
    static class SameTransactionAudit {
        private final AuditJpaRepository audits;
        private final Clock clock;

        SameTransactionAudit(AuditJpaRepository audits, Clock clock) {
            this.audits = audits;
            this.clock = clock;
        }

        @Transactional
        public void checkoutAuditSameTx(boolean fail) {
            audits.save(
                    new AuditRecordEntity("CHECKOUT_ATTEMPT", "same transaction", clock.instant()));
            if (fail) {
                throw new IllegalStateException("checkout failed after the audit write");
            }
        }
    }

    /** THE FIX: the entry goes through another bean's proxy, where REQUIRES_NEW is applied. */
    static class RequiresNewAudit {
        private final AuditLog auditLog;

        RequiresNewAudit(AuditLog auditLog) {
            this.auditLog = auditLog;
        }

        @Transactional
        public void checkoutAuditRequiresNew(boolean fail) {
            auditLog.record("CHECKOUT_ATTEMPT", "separate bean");
            if (fail) {
                throw new IllegalStateException("checkout failed after the audit write");
            }
        }
    }

    /**
     * THE FALSE FIX: REQUIRES_NEW on a method of the same bean is ignored, for the same reason as
     * trap 2.
     */
    static class SelfInvokedRequiresNewAudit {
        private final AuditJpaRepository audits;
        private final Clock clock;

        SelfInvokedRequiresNewAudit(AuditJpaRepository audits, Clock clock) {
            this.audits = audits;
            this.clock = clock;
        }

        @Transactional
        public void checkoutAuditSelfInvoked(boolean fail) {
            this.writeAudit();
            if (fail) {
                throw new IllegalStateException("checkout failed after the audit write");
            }
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void writeAudit() {
            audits.save(new AuditRecordEntity("CHECKOUT_ATTEMPT", "same bean", clock.instant()));
        }
    }
}
```

Reading it, one piece at a time:

- **`AuditTrapBeans`**, a class. Trap 3, planted three ways. Test code only; each variant fails a checkout after it wrote the audit entry.
  - annotation `@TestConfiguration(proxyBeanMethods = false)`: a configuration class that applies only in tests
  - **method `sameTransactionAudit`**: the signature is `SameTransactionAudit sameTransactionAudit(AuditJpaRepository audits, Clock clock)`.
    - Signature: `SameTransactionAudit sameTransactionAudit(AuditJpaRepository audits, Clock clock)`
    - annotation `@Bean`: tells Spring to call this method once at startup and keep the returned object as a shared, managed object
    - What the body does, in order:
      - returns `new SameTransactionAudit(audits, clock)`
  - **method `requiresNewAudit`**: the signature is `RequiresNewAudit requiresNewAudit(AuditLog auditLog)`.
    - Signature: `RequiresNewAudit requiresNewAudit(AuditLog auditLog)`
    - annotation `@Bean`: tells Spring to call this method once at startup and keep the returned object as a shared, managed object
    - What the body does, in order:
      - returns `new RequiresNewAudit(auditLog)`
  - **method `selfInvokedRequiresNewAudit`**: the signature is `SelfInvokedRequiresNewAudit selfInvokedRequiresNewAudit( AuditJpaRepository audits, Clock clock)`.
    - Signature: `SelfInvokedRequiresNewAudit selfInvokedRequiresNewAudit( AuditJpaRepository audits, Clock clock)`
    - annotation `@Bean`: tells Spring to call this method once at startup and keep the returned object as a shared, managed object
    - What the body does, in order:
      - returns `new SelfInvokedRequiresNewAudit(audits, clock)`
  - **`SameTransactionAudit`**, a class. THE BUG: the audit entry joins the checkout transaction and rolls back with it.
    - **field `audits`** of type `AuditJpaRepository`, set once when the object is built.
      - Declaration: `private final AuditJpaRepository audits`
    - **field `clock`** of type `Clock`, set once when the object is built.
      - Declaration: `private final Clock clock`
    - **constructor `SameTransactionAudit`**: the signature is `SameTransactionAudit(AuditJpaRepository audits, Clock clock)`.
      - Signature: `SameTransactionAudit(AuditJpaRepository audits, Clock clock)`
      - What the body does, in order:
        - stores `audits` in the field `audits`
        - stores `clock` in the field `clock`
    - **method `checkoutAuditSameTx`**: the signature is `void checkoutAuditSameTx(boolean fail)`.
      - Signature: `public void checkoutAuditSameTx(boolean fail)`
      - annotation `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
      - What the body does, in order:
        - calls `audits.save( new AuditRecordEntity("CHECKOUT_ATTEMPT", "same transaction", clock.instant()))`
        - when `fail` is true:
          - throws a new `IllegalStateException` created with `"checkout failed after the audit write"`
  - **`RequiresNewAudit`**, a class. THE FIX: the entry goes through another bean's proxy, where REQUIRES_NEW is applied.
    - **field `auditLog`** of type `AuditLog`, set once when the object is built.
      - Declaration: `private final AuditLog auditLog`
    - **constructor `RequiresNewAudit`**: the signature is `RequiresNewAudit(AuditLog auditLog)`.
      - Signature: `RequiresNewAudit(AuditLog auditLog)`
      - What the body does, in order:
        - stores `auditLog` in the field `auditLog`
    - **method `checkoutAuditRequiresNew`**: the signature is `void checkoutAuditRequiresNew(boolean fail)`.
      - Signature: `public void checkoutAuditRequiresNew(boolean fail)`
      - annotation `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
      - What the body does, in order:
        - calls `auditLog.record("CHECKOUT_ATTEMPT", "separate bean")`
        - when `fail` is true:
          - throws a new `IllegalStateException` created with `"checkout failed after the audit write"`
  - **`SelfInvokedRequiresNewAudit`**, a class. THE FALSE FIX: REQUIRES_NEW on a method of the same bean is ignored, for the same reason as trap 2.
    - **field `audits`** of type `AuditJpaRepository`, set once when the object is built.
      - Declaration: `private final AuditJpaRepository audits`
    - **field `clock`** of type `Clock`, set once when the object is built.
      - Declaration: `private final Clock clock`
    - **constructor `SelfInvokedRequiresNewAudit`**: the signature is `SelfInvokedRequiresNewAudit(AuditJpaRepository audits, Clock clock)`.
      - Signature: `SelfInvokedRequiresNewAudit(AuditJpaRepository audits, Clock clock)`
      - What the body does, in order:
        - stores `audits` in the field `audits`
        - stores `clock` in the field `clock`
    - **method `checkoutAuditSelfInvoked`**: the signature is `void checkoutAuditSelfInvoked(boolean fail)`.
      - Signature: `public void checkoutAuditSelfInvoked(boolean fail)`
      - annotation `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
      - What the body does, in order:
        - calls `this.writeAudit()`
        - when `fail` is true:
          - throws a new `IllegalStateException` created with `"checkout failed after the audit write"`
    - **method `writeAudit`**: the signature is `void writeAudit()`.
      - Signature: `public void writeAudit()`
      - annotation `@Transactional(propagation = Propagation.REQUIRES_NEW)`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
      - What the body does, in order:
        - calls `audits.save(new AuditRecordEntity("CHECKOUT_ATTEMPT", "same bean", clock.instant()))`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@TestConfiguration`: a configuration class that applies only in tests.
- `@Bean`: tells Spring to call this method once at startup and keep the returned object as a shared, managed object.
- `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back).

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.


### `src/test/java/dev/plainround/orders/traps/CheckoutIT.java`

Package `dev.plainround.orders.traps`. The fixed checkout, in production code, against real PostgreSQL: all of it or none of it, and no overselling.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.plainround.orders.application.CheckoutService;
import dev.plainround.orders.application.CustomerService;
import dev.plainround.orders.application.StockService;
import dev.plainround.orders.domain.CustomerNotFound;
import dev.plainround.orders.domain.Order;
import dev.plainround.orders.domain.OrderLine;
import dev.plainround.orders.domain.OutOfStock;
import dev.plainround.orders.domain.StockConflict;
import dev.plainround.orders.infrastructure.persistence.JpaStockRepository;
import dev.plainround.orders.infrastructure.persistence.StockItemEntity;
import dev.plainround.orders.infrastructure.persistence.StockJpaRepository;
import dev.plainround.orders.support.DatabaseReset;
import dev.plainround.orders.support.PostgresContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The fixed checkout, in production code, against real PostgreSQL: all of it or none of it, and no
 * overselling.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportTestcontainers(PostgresContainer.class)
class CheckoutIT {

    @Autowired private CheckoutService checkout;
    @Autowired private CustomerService customers;
    @Autowired private StockService stock;
    @Autowired private JpaStockRepository stockRepository;
    @Autowired private StockJpaRepository stockJpa;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void checkoutStoresTheOrderWithAllItsLinesAndTakesTheStock() {
        long customerId = customers.register("ada").id();
        stock.setAvailable("sku-1", 5);
        stock.setAvailable("sku-2", 2);

        Order placed =
                checkout.checkout(
                        customerId, List.of(new OrderLine("sku-1", 2), new OrderLine("sku-2", 1)));

        assertThat(placed.lines()).hasSize(2);
        assertThat(stock.available("sku-1")).isEqualTo(3);
        assertThat(stock.available("sku-2")).isEqualTo(1);
        assertThat(DatabaseReset.count(jdbc, "order_lines")).isEqualTo(2);
    }

    @Test
    void aLaterLineOutOfStockRollsBackTheOrderAndTheEarlierReservationButKeepsTheAudit() {
        long customerId = customers.register("ada").id();
        stock.setAvailable("sku-1", 5);
        stock.setAvailable("sku-2", 0);

        assertThatThrownBy(
                        () ->
                                checkout.checkout(
                                        customerId,
                                        List.of(
                                                new OrderLine("sku-1", 2),
                                                new OrderLine("sku-2", 1))))
                .isInstanceOf(OutOfStock.class);

        assertThat(DatabaseReset.count(jdbc, "orders_table")).isZero();
        assertThat(DatabaseReset.count(jdbc, "order_lines")).isZero();
        assertThat(stock.available("sku-1")).as("the earlier reservation rolled back").isEqualTo(5);
        assertThat(DatabaseReset.count(jdbc, "audit_records"))
                .as("the attempt is still on record")
                .isEqualTo(1);
    }

    @Test
    void anUnknownCustomerLeavesOnlyTheAuditEntry() {
        stock.setAvailable("sku-1", 5);

        assertThatThrownBy(() -> checkout.checkout(999, List.of(new OrderLine("sku-1", 1))))
                .isInstanceOf(CustomerNotFound.class);

        assertThat(DatabaseReset.count(jdbc, "orders_table")).isZero();
        assertThat(DatabaseReset.count(jdbc, "audit_records")).isEqualTo(1);
    }

    @Test
    void aTransactionThatReadAStaleStockVersionLosesTheRaceInsteadOfOverwriting() {
        stock.setAvailable("sku-1", 5);
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        outer.executeWithoutResult(
                status -> {
                    StockItemEntity stale = stockJpa.findById("sku-1").orElseThrow();
                    inner.executeWithoutResult(other -> stockRepository.reserve("sku-1", 1));
                    assertThat(stale.getVersion()).isZero();

                    assertThatThrownBy(() -> stockRepository.reserve("sku-1", 1))
                            .isInstanceOf(StockConflict.class);
                    status.setRollbackOnly();
                });

        assertThat(stock.available("sku-1")).as("only the first reservation was kept").isEqualTo(4);
    }

    @Test
    void racingCheckoutsForTheLastUnitsNeverOversell() throws Exception {
        long customerId = customers.register("ada").id();
        stock.setAvailable("sku-race", 3);
        AtomicInteger sold = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> attempts = new ArrayList<>();
            for (int attempt = 0; attempt < 8; attempt++) {
                attempts.add(
                        pool.submit(
                                () -> {
                                    try {
                                        checkout.checkout(
                                                customerId, List.of(new OrderLine("sku-race", 1)));
                                        sold.incrementAndGet();
                                    } catch (OutOfStock exception) {
                                        outOfStock.incrementAndGet();
                                    } catch (StockConflict exception) {
                                        conflicts.incrementAndGet();
                                    }
                                }));
            }
            for (Future<?> attempt : attempts) {
                attempt.get();
            }
        }

        assertThat(sold.get() + outOfStock.get() + conflicts.get()).isEqualTo(8);
        assertThat(sold.get()).isBetween(1, 3);
        assertThat(stock.available("sku-race")).isEqualTo(3 - sold.get());
        assertThat(DatabaseReset.count(jdbc, "orders_table")).isEqualTo(sold.get());
    }
}
```

Reading it, one piece at a time:

- **`CheckoutIT`**, a class. The fixed checkout, in production code, against real PostgreSQL: all of it or none of it, and no overselling.
  - annotation `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)`: starts the whole application inside the test (all beans, the real configuration) so the test can use it
  - annotation `@ImportTestcontainers(PostgresContainer.class)`: starts the containers declared in the named class for this test and wires them into the Spring configuration
  - **field `checkout`** of type `CheckoutService`, set once when the object is built.
    - Declaration: `private CheckoutService checkout`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `customers`** of type `CustomerService`, set once when the object is built.
    - Declaration: `private CustomerService customers`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `stock`** of type `StockService`, set once when the object is built.
    - Declaration: `private StockService stock`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `stockRepository`** of type `JpaStockRepository`, set once when the object is built.
    - Declaration: `private JpaStockRepository stockRepository`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `stockJpa`** of type `StockJpaRepository`, set once when the object is built.
    - Declaration: `private StockJpaRepository stockJpa`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `transactionManager`** of type `PlatformTransactionManager`, set once when the object is built.
    - Declaration: `private PlatformTransactionManager transactionManager`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `jdbc`** of type `JdbcTemplate`, set once when the object is built.
    - Declaration: `private JdbcTemplate jdbc`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **method `clean`**: the signature is `void clean()`.
    - Signature: `void clean()`
    - annotation `@AfterEach`: runs this method after every test, to clean up
    - What the body does, in order:
      - calls `DatabaseReset.truncate(jdbc)`
  - **test `checkoutStoresTheOrderWithAllItsLinesAndTakesTheStock`**: reads as "checkout stores the order with all its lines and takes the stock"
    - Signature: `void checkoutStoresTheOrderWithAllItsLinesAndTakesTheStock()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customerId` (of type `long`) holding `customers.register("ada").id()`
      - calls `stock.setAvailable("sku-1", 5)`
      - calls `stock.setAvailable("sku-2", 2)`
      - creates the variable `placed` (of type `Order`) holding `checkout.checkout( customerId, List.of(new OrderLine("sku-1", 2), new OrderLine("sku-2", 1)))`
      - checks that `placed.lines()` has the size `2`
      - checks that `stock.available("sku-1")` equals `3`
      - checks that `stock.available("sku-2")` equals `1`
      - checks that `DatabaseReset.count(jdbc, "order_lines")` equals `2`
  - **test `aLaterLineOutOfStockRollsBackTheOrderAndTheEarlierReservationButKeepsTheAudit`**: reads as "a later line out of stock rolls back the order and the earlier reservation but keeps the audit"
    - Signature: `void aLaterLineOutOfStockRollsBackTheOrderAndTheEarlierReservationButKeepsTheAudit()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customerId` (of type `long`) holding `customers.register("ada").id()`
      - calls `stock.setAvailable("sku-1", 5)`
      - calls `stock.setAvailable("sku-2", 0)`
      - checks that running `checkout.checkout( customerId, List.of( new OrderLine("sku-1", 2), new OrderLine("sku-2", 1)))` fails
        - the failure is an instance of `OutOfStock.class`
      - checks that `DatabaseReset.count(jdbc, "orders_table")` is zero
      - checks that `DatabaseReset.count(jdbc, "order_lines")` is zero
      - checks that `stock.available("sku-1")` as `"the earlier reservation rolled back"`
      - and that it equals `5`
      - checks that `DatabaseReset.count(jdbc, "audit_records")` as `"the attempt is still on record"`
      - and that it equals `1`
  - **test `anUnknownCustomerLeavesOnlyTheAuditEntry`**: reads as "an unknown customer leaves only the audit entry"
    - Signature: `void anUnknownCustomerLeavesOnlyTheAuditEntry()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - calls `stock.setAvailable("sku-1", 5)`
      - checks that running `checkout.checkout(999, List.of(new OrderLine("sku-1", 1)))` fails
        - the failure is an instance of `CustomerNotFound.class`
      - checks that `DatabaseReset.count(jdbc, "orders_table")` is zero
      - checks that `DatabaseReset.count(jdbc, "audit_records")` equals `1`
  - **test `aTransactionThatReadAStaleStockVersionLosesTheRaceInsteadOfOverwriting`**: reads as "a transaction that read a stale stock version loses the race instead of overwriting"
    - Signature: `void aTransactionThatReadAStaleStockVersionLosesTheRaceInsteadOfOverwriting()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - calls `stock.setAvailable("sku-1", 5)`
      - creates the variable `outer` (of type `TransactionTemplate`) holding `new TransactionTemplate(transactionManager)`
      - creates the variable `inner` (of type `TransactionTemplate`) holding `new TransactionTemplate(transactionManager)`
      - calls `inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW)`
      - calls `outer.executeWithoutResult( status -> { StockItemEntity stale = stockJpa.findById("sku-1").orElseThrow(); inner.executeWithoutResult(other -> stockRepository.reserve("sku-1", 1)); assertThat(stale.getVersion()).isZero(); assertThatThrownBy(() -> stockRepository.reserve("sku-1", 1)) .isInstanceOf(StockConflict.class); status.setRollbackOnly(); })`
      - checks that `stock.available("sku-1")` as `"only the first reservation was kept"`
      - and that it equals `4`
  - **test `racingCheckoutsForTheLastUnitsNeverOversell`**: reads as "racing checkouts for the last units never oversell"
    - Signature: `void racingCheckoutsForTheLastUnitsNeverOversell() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customerId` (of type `long`) holding `customers.register("ada").id()`
      - calls `stock.setAvailable("sku-race", 3)`
      - creates the variable `sold` (of type `AtomicInteger`) holding `new AtomicInteger()`
      - creates the variable `outOfStock` (of type `AtomicInteger`) holding `new AtomicInteger()`
      - creates the variable `conflicts` (of type `AtomicInteger`) holding `new AtomicInteger()`
      - tries this (managing `ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()` so it is closed afterwards):
        - creates the variable `attempts` (of type `List<Future<?>>`) holding `new ArrayList<>()`
        - repeats for `int attempt = 0; attempt < 8; attempt++`:
          - calls `attempts.add( pool.submit( () -> { try { checkout.checkout( customerId, List.of(new OrderLine("sku-race", 1))); sold.incrementAndGet(); } catch (OutOfStock exception) { outOfStock.incrementAndGet(); } catch (StockConflict exception) { conflicts.incrementAndGet(); } }))`
        - repeats for `Future<?> attempt : attempts`:
          - calls `attempt.get()`
      - checks that `sold.get() + outOfStock.get() + conflicts.get()` equals `8`
      - checks that `sold.get()` lies between `1, 3`
      - checks that `stock.available("sku-race")` equals `3 - sold.get()`
      - checks that `DatabaseReset.count(jdbc, "orders_table")` equals `sold.get()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@SpringBootTest`: starts the whole application inside the test (all beans, the real configuration) so the test can use it.
- `@ImportTestcontainers`: starts the containers declared in the named class for this test and wires them into the Spring configuration.
- `@Autowired`: asks Spring to supply the object this field or parameter needs.
- `@AfterEach`: runs this method after every test, to clean up.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **throws**: `throws X` in a signature says the method may fail with exception `X`; an exception is an object that stops normal flow and travels up the call stack until something catches it.
- **try/catch**: `try { ... } catch (X e) { ... }` runs the first block and, if it throws `X`, runs the second.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.
- **`assertThatThrownBy`**: `assertThatThrownBy(() -> code)` runs the code and checks that it throws; the chained calls describe the exception expected.


### `src/test/java/dev/plainround/orders/traps/LazyEndpointConfig.java`

Package `dev.plainround.orders.traps`. A test-only endpoint that touches a lazy collection while building the response, outside any transaction, the way a controller that returns an entity would.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import dev.plainround.orders.infrastructure.persistence.OrderJpaRepository;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * A test-only endpoint that touches a lazy collection while building the response, outside any
 * transaction, the way a controller that returns an entity would. It is a router function, not an
 * annotated controller, so no component scan can pick it up.
 */
@TestConfiguration(proxyBeanMethods = false)
class LazyEndpointConfig {

    @Bean
    RouterFunction<ServerResponse> lazyLines(OrderJpaRepository orders) {
        return RouterFunctions.route()
                .GET(
                        "/test/lazy/{id}",
                        request -> {
                            OrderEntity order =
                                    orders.findById(Long.parseLong(request.pathVariable("id")))
                                            .orElseThrow();
                            return ServerResponse.ok()
                                    .body(Map.of("lines", order.getLines().size()));
                        })
                .build();
    }
}
```

Reading it, one piece at a time:

- **`LazyEndpointConfig`**, a class. A test-only endpoint that touches a lazy collection while building the response, outside any transaction, the way a controller that returns an entity would. It is a router function, not an annotated controller, so no component scan can pick it up.
  - annotation `@TestConfiguration(proxyBeanMethods = false)`: a configuration class that applies only in tests
  - **method `lazyLines`**: the signature is `RouterFunction<ServerResponse> lazyLines(OrderJpaRepository orders)`.
    - Signature: `RouterFunction<ServerResponse> lazyLines(OrderJpaRepository orders)`
    - annotation `@Bean`: tells Spring to call this method once at startup and keep the returned object as a shared, managed object
    - What the body does, in order:
      - returns `RouterFunctions.route() .GET( "/test/lazy/{id}", request -> { OrderEntity order = orders.findById(Long.parseLong(request.pathVariable("id"))) .orElseThrow(); return ServerResponse.ok() .body(Map.of("lines", order.getLines().size())); }) .build()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@TestConfiguration`: a configuration class that applies only in tests.
- `@Bean`: tells Spring to call this method once at startup and keep the returned object as a shared, managed object.

Language features in this file, explained:

- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **Map**: A `Map` stores values under keys, like a dictionary.


### `src/test/java/dev/plainround/orders/traps/NPlusOneIT.java`

Package `dev.plainround.orders.traps`. Trap 1, the N+1: the naive listing grows with the number of orders, the fetch-joined one does not.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plainround.orders.domain.Order;
import dev.plainround.orders.infrastructure.persistence.CustomerEntity;
import dev.plainround.orders.infrastructure.persistence.JpaOrderRepository;
import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import dev.plainround.orders.infrastructure.persistence.OrderLineEntity;
import dev.plainround.orders.support.Evidence;
import dev.plainround.orders.support.PostgresContainer;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;

/**
 * Trap 1, the N+1: the naive listing grows with the number of orders, the fetch-joined one does
 * not. The statement count is asserted, not read off a log.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(PostgresContainer.class)
@Import(JpaOrderRepository.class)
class NPlusOneIT {

    @Autowired private EntityManager entityManager;
    @Autowired private NaiveOrderJpaRepository naive;
    @Autowired private JpaOrderRepository fixed;

    private Statistics statistics;

    @BeforeEach
    void statistics() {
        statistics =
                entityManager
                        .getEntityManagerFactory()
                        .unwrap(SessionFactory.class)
                        .getStatistics();
    }

    @Test
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

    @Test
    void theNaiveCountKeepsGrowingWith200Orders() {
        long customerId = seedCustomerWithOrders(200);

        naive.findByCustomerId(customerId).forEach(order -> order.getLines().size());

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + 200);
        Evidence.record("naive queries for 200 orders: " + statistics.getPrepareStatementCount());
    }

    @Test
    void fetchJoinLoadsTwentyOrdersWithTheirLinesInOneStatement() {
        long customerId = seedCustomerWithOrders(20);

        List<Order> orders = fixed.findByCustomerId(customerId);

        assertThat(orders).hasSize(20);
        assertThat(orders).allSatisfy(order -> assertThat(order.lines()).hasSize(3));
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
        Evidence.record("fixed queries for 20 orders:  " + statistics.getPrepareStatementCount());
    }

    @Test
    void fetchJoinKeepsQueryCountFlatAt200Orders() {
        long customerId = seedCustomerWithOrders(200);

        List<Order> orders = fixed.findByCustomerId(customerId);

        assertThat(orders).hasSize(200);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
        Evidence.record("fixed queries for 200 orders: " + statistics.getPrepareStatementCount());
    }

    private long seedCustomerWithOrders(int orderCount) {
        CustomerEntity customer = new CustomerEntity("ada");
        for (int orderIndex = 0; orderIndex < orderCount; orderIndex++) {
            OrderEntity order =
                    new OrderEntity(Instant.parse("2026-10-27T10:00:00Z").plusSeconds(orderIndex));
            customer.addOrder(order);
            for (int lineIndex = 0; lineIndex < 3; lineIndex++) {
                order.addLine(new OrderLineEntity("sku-" + orderIndex + "-" + lineIndex, 1));
            }
        }
        entityManager.persist(customer);
        entityManager.flush();
        entityManager.clear();
        statistics.clear();
        return customer.getId();
    }
}
```

Reading it, one piece at a time:

- **`NPlusOneIT`**, a class. Trap 1, the N+1: the naive listing grows with the number of orders, the fetch-joined one does not. The statement count is asserted, not read off a log.
  - annotation `@DataJpaTest`: starts only the JPA layer (entities, repositories, a database) for a test
  - annotation `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`: controls whether the test replaces the configured database with an embedded one
  - annotation `@ImportTestcontainers(PostgresContainer.class)`: starts the containers declared in the named class for this test and wires them into the Spring configuration
  - annotation `@Import(JpaOrderRepository.class)`: adds the named configuration classes to the Spring context
  - **field `entityManager`** of type `EntityManager`, set once when the object is built.
    - Declaration: `private EntityManager entityManager`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `naive`** of type `NaiveOrderJpaRepository`, set once when the object is built.
    - Declaration: `private NaiveOrderJpaRepository naive`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `fixed`** of type `JpaOrderRepository`, set once when the object is built.
    - Declaration: `private JpaOrderRepository fixed`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `statistics`** of type `Statistics`, set once when the object is built.
    - Declaration: `private Statistics statistics`
  - **method `statistics`**: the signature is `void statistics()`.
    - Signature: `void statistics()`
    - annotation `@BeforeEach`: runs this method before every test, to prepare a known starting state
    - What the body does, in order:
      - sets `statistics` to `entityManager .getEntityManagerFactory() .unwrap(SessionFactory.class) .getStatistics()`
  - **test `theNaiveListingRunsOneQueryForTheOrdersAndOneMorePerOrderForItsLines`**: reads as "the naive listing runs one query for the orders and one more per order for its lines"
    - Signature: `void theNaiveListingRunsOneQueryForTheOrdersAndOneMorePerOrderForItsLines()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customerId` (of type `long`) holding `seedCustomerWithOrders(20)`
      - creates the variable `lines` (of type `int`) holding `naive.findByCustomerId(customerId).stream() .mapToInt(order -> order.getLines().size()) .sum()`
      - checks that `lines` equals `60`
      - checks that `statistics.getPrepareStatementCount()` equals `1 + 20`
      - calls `Evidence.record("naive queries for 20 orders: " + statistics.getPrepareStatementCount())`
  - **test `theNaiveCountKeepsGrowingWith200Orders`**: reads as "the naive count keeps growing with200 orders"
    - Signature: `void theNaiveCountKeepsGrowingWith200Orders()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customerId` (of type `long`) holding `seedCustomerWithOrders(200)`
      - calls `naive.findByCustomerId(customerId).forEach(order -> order.getLines().size())`
      - checks that `statistics.getPrepareStatementCount()` equals `1 + 200`
      - calls `Evidence.record("naive queries for 200 orders: " + statistics.getPrepareStatementCount())`
  - **test `fetchJoinLoadsTwentyOrdersWithTheirLinesInOneStatement`**: reads as "fetch join loads twenty orders with their lines in one statement"
    - Signature: `void fetchJoinLoadsTwentyOrdersWithTheirLinesInOneStatement()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customerId` (of type `long`) holding `seedCustomerWithOrders(20)`
      - creates the variable `orders` (of type `List<Order>`) holding `fixed.findByCustomerId(customerId)`
      - checks that `orders` has the size `20`
      - checks that `orders` all satisfy `order -> assertThat(order.lines()).hasSize(3)`
      - checks that `statistics.getPrepareStatementCount()` is at most `2`
      - calls `Evidence.record("fixed queries for 20 orders: " + statistics.getPrepareStatementCount())`
  - **test `fetchJoinKeepsQueryCountFlatAt200Orders`**: reads as "fetch join keeps query count flat at200 orders"
    - Signature: `void fetchJoinKeepsQueryCountFlatAt200Orders()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customerId` (of type `long`) holding `seedCustomerWithOrders(200)`
      - creates the variable `orders` (of type `List<Order>`) holding `fixed.findByCustomerId(customerId)`
      - checks that `orders` has the size `200`
      - checks that `statistics.getPrepareStatementCount()` is at most `2`
      - calls `Evidence.record("fixed queries for 200 orders: " + statistics.getPrepareStatementCount())`
  - **method `seedCustomerWithOrders`**: the signature is `long seedCustomerWithOrders(int orderCount)`.
    - Signature: `private long seedCustomerWithOrders(int orderCount)`
    - What the body does, in order:
      - creates the variable `customer` (of type `CustomerEntity`) holding `new CustomerEntity("ada")`
      - repeats for `int orderIndex = 0; orderIndex < orderCount; orderIndex++`:
        - creates the variable `order` (of type `OrderEntity`) holding `new OrderEntity(Instant.parse("2026-10-27T10:00:00Z").plusSeconds(orderIndex))`
        - calls `customer.addOrder(order)`
        - repeats for `int lineIndex = 0; lineIndex < 3; lineIndex++`:
          - calls `order.addLine(new OrderLineEntity("sku-" + orderIndex + "-" + lineIndex, 1))`
      - calls `entityManager.persist(customer)`
      - calls `entityManager.flush()`
      - calls `entityManager.clear()`
      - calls `statistics.clear()`
      - returns `customer.getId()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@DataJpaTest`: starts only the JPA layer (entities, repositories, a database) for a test.
- `@AutoConfigureTestDatabase`: controls whether the test replaces the configured database with an embedded one.
- `@ImportTestcontainers`: starts the containers declared in the named class for this test and wires them into the Spring configuration.
- `@Import`: adds the named configuration classes to the Spring context.
- `@Autowired`: asks Spring to supply the object this field or parameter needs.
- `@BeforeEach`: runs this method before every test, to prepare a known starting state.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **Stream**: A stream processes a collection step by step: `map` converts each item, `filter` keeps some, `toList` collects the result.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.


### `src/test/java/dev/plainround/orders/traps/NaiveOrderJpaRepository.java`

Package `dev.plainround.orders.traps`. Trap 1, as a reviewer would see it: a derived query that looks harmless and loads the lines lazily.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Trap 1, as a reviewer would see it: a derived query that looks harmless and loads the lines
 * lazily.
 */
public interface NaiveOrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * One query for the orders; every access to {@code getLines()} afterwards costs one more.
     *
     * @param customerId the buyer
     * @return the orders with lazy lines
     */
    List<OrderEntity> findByCustomerId(Long customerId);
}
```

Reading it, one piece at a time:

- **`NaiveOrderJpaRepository`**, a interface that extends `JpaRepository<OrderEntity, Long>`. Trap 1, as a reviewer would see it: a derived query that looks harmless and loads the lines lazily.
  - **method `findByCustomerId`**: One query for the orders; every access to `getLines()` afterwards costs one more.
    - Signature: `List<OrderEntity> findByCustomerId(Long customerId)`
    - input `customerId`: the buyer
    - gives back: the orders with lazy lines

Language features in this file, explained:

- **interface**: An `interface` lists method signatures without bodies. Any class that says it `implements` the interface must provide those methods, so callers can depend on the list alone.
- **public**: `public` lets any class use the member.
- **extends**: `extends X` makes the class a child of `X`: it inherits what `X` has and may replace parts.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.


### `src/test/java/dev/plainround/orders/traps/OpenInViewIT.java`

Package `dev.plainround.orders.traps`. With the configured `open-in-view=false`, a lazy load in the response step fails where it can be seen.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plainround.orders.infrastructure.persistence.CustomerEntity;
import dev.plainround.orders.infrastructure.persistence.CustomerJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import dev.plainround.orders.infrastructure.persistence.OrderLineEntity;
import dev.plainround.orders.support.DatabaseReset;
import dev.plainround.orders.support.Evidence;
import dev.plainround.orders.support.PostgresContainer;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * With the configured {@code open-in-view=false}, a lazy load in the response step fails where it
 * can be seen.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportTestcontainers(PostgresContainer.class)
@Import(LazyEndpointConfig.class)
class OpenInViewIT {

    @Value("${local.server.port}")
    private int port;

    @Autowired private CustomerJpaRepository customers;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void aLazyLoadOutsideATransactionFailsWhenOpenInViewIsOff() {
        long orderId = seedOrderWithTwoLines();

        int status = RestClientSupport.statusOf(port, "/test/lazy/" + orderId);

        assertThat(status).isEqualTo(500);
        Evidence.record("lazy load in the response step, open-in-view=false: HTTP " + status);
    }

    private long seedOrderWithTwoLines() {
        CustomerEntity customer = new CustomerEntity("ada");
        OrderEntity order = new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"));
        customer.addOrder(order);
        order.addLine(new OrderLineEntity("sku-1", 1));
        order.addLine(new OrderLineEntity("sku-2", 1));
        return customers.saveAndFlush(customer).getOrders().get(0).getId();
    }

    /** Small HTTP helper shared with the open-in-view-on variant. */
    static final class RestClientSupport {
        private RestClientSupport() {}

        static int statusOf(int port, String path) {
            return RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {})
                    .build()
                    .get()
                    .uri(path)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .value();
        }
    }
}
```

Reading it, one piece at a time:

- **`OpenInViewIT`**, a class. With the configured `open-in-view=false`, a lazy load in the response step fails where it can be seen.
  - annotation `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`: starts the whole application inside the test (all beans, the real configuration) so the test can use it
  - annotation `@ImportTestcontainers(PostgresContainer.class)`: starts the containers declared in the named class for this test and wires them into the Spring configuration
  - annotation `@Import(LazyEndpointConfig.class)`: adds the named configuration classes to the Spring context
  - **method `port`**: the signature is `int port`.
    - Signature: `private int port`
    - annotation `@Value("${local.server.port}")`: injects a value from the configuration into this field or parameter
  - **field `customers`** of type `CustomerJpaRepository`, set once when the object is built.
    - Declaration: `private CustomerJpaRepository customers`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `jdbc`** of type `JdbcTemplate`, set once when the object is built.
    - Declaration: `private JdbcTemplate jdbc`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **method `clean`**: the signature is `void clean()`.
    - Signature: `void clean()`
    - annotation `@AfterEach`: runs this method after every test, to clean up
    - What the body does, in order:
      - calls `DatabaseReset.truncate(jdbc)`
  - **test `aLazyLoadOutsideATransactionFailsWhenOpenInViewIsOff`**: reads as "a lazy load outside a transaction fails when open in view is off"
    - Signature: `void aLazyLoadOutsideATransactionFailsWhenOpenInViewIsOff()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `orderId` (of type `long`) holding `seedOrderWithTwoLines()`
      - creates the variable `status` (of type `int`) holding `RestClientSupport.statusOf(port, "/test/lazy/" + orderId)`
      - checks that `status` equals `500`
      - calls `Evidence.record("lazy load in the response step, open-in-view=false: HTTP " + status)`
  - **method `seedOrderWithTwoLines`**: the signature is `long seedOrderWithTwoLines()`.
    - Signature: `private long seedOrderWithTwoLines()`
    - What the body does, in order:
      - creates the variable `customer` (of type `CustomerEntity`) holding `new CustomerEntity("ada")`
      - creates the variable `order` (of type `OrderEntity`) holding `new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"))`
      - calls `customer.addOrder(order)`
      - calls `order.addLine(new OrderLineEntity("sku-1", 1))`
      - calls `order.addLine(new OrderLineEntity("sku-2", 1))`
      - returns `customers.saveAndFlush(customer).getOrders().get(0).getId()`
  - **`RestClientSupport`**, a class. Small HTTP helper shared with the open-in-view-on variant.
    - **method `RestClientSupport`**: the signature is `RestClientSupport()`.
      - Signature: `private RestClientSupport()`
    - **method `statusOf`**: the signature is `static int statusOf(int port, String path)`.
      - Signature: `static int statusOf(int port, String path)`
      - What the body does, in order:
        - returns `RestClient.builder() .baseUrl("http://localhost:" + port) .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {}) .build() .get() .uri(path) .retrieve() .toBodilessEntity() .getStatusCode() .value()`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@SpringBootTest`: starts the whole application inside the test (all beans, the real configuration) so the test can use it.
- `@ImportTestcontainers`: starts the containers declared in the named class for this test and wires them into the Spring configuration.
- `@Import`: adds the named configuration classes to the Spring context.
- `@Value`: injects a value from the configuration into this field or parameter.
- `@Autowired`: asks Spring to supply the object this field or parameter needs.
- `@AfterEach`: runs this method after every test, to clean up.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **method reference (`::`)**: `Type::method` is a short way to pass an existing method where a function is expected.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.


### `src/test/java/dev/plainround/orders/traps/OpenInViewOnIT.java`

Package `dev.plainround.orders.traps`. Stretch 3: the same endpoint with Spring Boot's default turned back on.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plainround.orders.infrastructure.persistence.CustomerEntity;
import dev.plainround.orders.infrastructure.persistence.CustomerJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import dev.plainround.orders.infrastructure.persistence.OrderLineEntity;
import dev.plainround.orders.support.DatabaseReset;
import dev.plainround.orders.support.Evidence;
import dev.plainround.orders.support.PostgresContainer;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Stretch 3: the same endpoint with Spring Boot's default turned back on. The bug it hid now
 * answers 200.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.open-in-view=true")
@ImportTestcontainers(PostgresContainer.class)
@Import(LazyEndpointConfig.class)
class OpenInViewOnIT {

    @Value("${local.server.port}")
    private int port;

    @Autowired private CustomerJpaRepository customers;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void openInViewOnHidesTheLazyLoadByKeepingTheSessionOpenForTheWholeRequest() {
        CustomerEntity customer = new CustomerEntity("ada");
        OrderEntity order = new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"));
        customer.addOrder(order);
        order.addLine(new OrderLineEntity("sku-1", 1));
        long orderId = customers.saveAndFlush(customer).getOrders().get(0).getId();

        int status = OpenInViewIT.RestClientSupport.statusOf(port, "/test/lazy/" + orderId);

        assertThat(status).isEqualTo(200);
        Evidence.record(
                "lazy load in the response step, open-in-view=true:  HTTP "
                        + status
                        + "   (the bug is hidden)");
    }
}
```

Reading it, one piece at a time:

- **`OpenInViewOnIT`**, a class. Stretch 3: the same endpoint with Spring Boot's default turned back on. The bug it hid now answers 200.
  - annotation `@SpringBootTest( webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.jpa.open-in-view=true")`: starts the whole application inside the test (all beans, the real configuration) so the test can use it
  - annotation `@ImportTestcontainers(PostgresContainer.class)`: starts the containers declared in the named class for this test and wires them into the Spring configuration
  - annotation `@Import(LazyEndpointConfig.class)`: adds the named configuration classes to the Spring context
  - **method `port`**: the signature is `int port`.
    - Signature: `private int port`
    - annotation `@Value("${local.server.port}")`: injects a value from the configuration into this field or parameter
  - **field `customers`** of type `CustomerJpaRepository`, set once when the object is built.
    - Declaration: `private CustomerJpaRepository customers`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `jdbc`** of type `JdbcTemplate`, set once when the object is built.
    - Declaration: `private JdbcTemplate jdbc`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **method `clean`**: the signature is `void clean()`.
    - Signature: `void clean()`
    - annotation `@AfterEach`: runs this method after every test, to clean up
    - What the body does, in order:
      - calls `DatabaseReset.truncate(jdbc)`
  - **test `openInViewOnHidesTheLazyLoadByKeepingTheSessionOpenForTheWholeRequest`**: reads as "open in view on hides the lazy load by keeping the session open for the whole request"
    - Signature: `void openInViewOnHidesTheLazyLoadByKeepingTheSessionOpenForTheWholeRequest()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customer` (of type `CustomerEntity`) holding `new CustomerEntity("ada")`
      - creates the variable `order` (of type `OrderEntity`) holding `new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"))`
      - calls `customer.addOrder(order)`
      - calls `order.addLine(new OrderLineEntity("sku-1", 1))`
      - creates the variable `orderId` (of type `long`) holding `customers.saveAndFlush(customer).getOrders().get(0).getId()`
      - creates the variable `status` (of type `int`) holding `OpenInViewIT.RestClientSupport.statusOf(port, "/test/lazy/" + orderId)`
      - checks that `status` equals `200`
      - calls `Evidence.record( "lazy load in the response step, open-in-view=true: HTTP " + status + " (the bug is hidden)")`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@SpringBootTest`: starts the whole application inside the test (all beans, the real configuration) so the test can use it.
- `@ImportTestcontainers`: starts the containers declared in the named class for this test and wires them into the Spring configuration.
- `@Import`: adds the named configuration classes to the Spring context.
- `@Value`: injects a value from the configuration into this field or parameter.
- `@Autowired`: asks Spring to supply the object this field or parameter needs.
- `@AfterEach`: runs this method after every test, to clean up.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.


### `src/test/java/dev/plainround/orders/traps/OrderModelIT.java`

Package `dev.plainround.orders.traps`. Steps 1 and 2: the model works on real PostgreSQL, and a test can count the statements Hibernate ran.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plainround.orders.infrastructure.persistence.CustomerEntity;
import dev.plainround.orders.infrastructure.persistence.CustomerJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import dev.plainround.orders.infrastructure.persistence.OrderJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderLineEntity;
import dev.plainround.orders.support.Evidence;
import dev.plainround.orders.support.PostgresContainer;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.core.env.Environment;

/**
 * Steps 1 and 2: the model works on real PostgreSQL, and a test can count the statements Hibernate
 * ran.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(PostgresContainer.class)
class OrderModelIT {

    @Autowired private CustomerJpaRepository customers;
    @Autowired private OrderJpaRepository orders;
    @Autowired private EntityManager entityManager;
    @Autowired private Environment environment;

    @Test
    void savesAndReadsBackACustomerWithTwoOrdersOfThreeLinesEach() {
        CustomerEntity customer = new CustomerEntity("ada");
        for (int orderIndex = 0; orderIndex < 2; orderIndex++) {
            OrderEntity order = new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"));
            customer.addOrder(order);
            for (int lineIndex = 0; lineIndex < 3; lineIndex++) {
                order.addLine(new OrderLineEntity("sku-" + orderIndex + "-" + lineIndex, 1));
            }
        }
        customers.saveAndFlush(customer);
        entityManager.clear();

        CustomerEntity reloaded = customers.findById(customer.getId()).orElseThrow();

        assertThat(reloaded.getOrders()).hasSize(2);
        assertThat(reloaded.getOrders().get(0).getLines()).hasSize(3);
    }

    @Test
    void loadingOneOrderByIdIsExactlyOneStatement() {
        CustomerEntity customer = new CustomerEntity("ada");
        OrderEntity order = new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"));
        customer.addOrder(order);
        order.addLine(new OrderLineEntity("sku-1", 1));
        customers.saveAndFlush(customer);
        entityManager.clear();
        Statistics statistics =
                entityManager
                        .getEntityManagerFactory()
                        .unwrap(SessionFactory.class)
                        .getStatistics();
        statistics.clear();

        orders.findById(order.getId());

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        Evidence.record(
                "loadingOneOrderByIdIsExactlyOneStatement: prepareStatementCount = "
                        + statistics.getPrepareStatementCount());
    }

    @Test
    void openInViewIsExplicitlyOffInTheApplicationConfiguration() {
        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }
}
```

Reading it, one piece at a time:

- **`OrderModelIT`**, a class. Steps 1 and 2: the model works on real PostgreSQL, and a test can count the statements Hibernate ran.
  - annotation `@DataJpaTest`: starts only the JPA layer (entities, repositories, a database) for a test
  - annotation `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`: controls whether the test replaces the configured database with an embedded one
  - annotation `@ImportTestcontainers(PostgresContainer.class)`: starts the containers declared in the named class for this test and wires them into the Spring configuration
  - **field `customers`** of type `CustomerJpaRepository`, set once when the object is built.
    - Declaration: `private CustomerJpaRepository customers`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `orders`** of type `OrderJpaRepository`, set once when the object is built.
    - Declaration: `private OrderJpaRepository orders`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `entityManager`** of type `EntityManager`, set once when the object is built.
    - Declaration: `private EntityManager entityManager`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `environment`** of type `Environment`, set once when the object is built.
    - Declaration: `private Environment environment`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **test `savesAndReadsBackACustomerWithTwoOrdersOfThreeLinesEach`**: reads as "saves and reads back a customer with two orders of three lines each"
    - Signature: `void savesAndReadsBackACustomerWithTwoOrdersOfThreeLinesEach()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customer` (of type `CustomerEntity`) holding `new CustomerEntity("ada")`
      - repeats for `int orderIndex = 0; orderIndex < 2; orderIndex++`:
        - creates the variable `order` (of type `OrderEntity`) holding `new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"))`
        - calls `customer.addOrder(order)`
        - repeats for `int lineIndex = 0; lineIndex < 3; lineIndex++`:
          - calls `order.addLine(new OrderLineEntity("sku-" + orderIndex + "-" + lineIndex, 1))`
      - calls `customers.saveAndFlush(customer)`
      - calls `entityManager.clear()`
      - creates the variable `reloaded` (of type `CustomerEntity`) holding `customers.findById(customer.getId()).orElseThrow()`
      - checks that `reloaded.getOrders()` has the size `2`
      - checks that `reloaded.getOrders().get(0).getLines()` has the size `3`
  - **test `loadingOneOrderByIdIsExactlyOneStatement`**: reads as "loading one order by id is exactly one statement"
    - Signature: `void loadingOneOrderByIdIsExactlyOneStatement()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customer` (of type `CustomerEntity`) holding `new CustomerEntity("ada")`
      - creates the variable `order` (of type `OrderEntity`) holding `new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"))`
      - calls `customer.addOrder(order)`
      - calls `order.addLine(new OrderLineEntity("sku-1", 1))`
      - calls `customers.saveAndFlush(customer)`
      - calls `entityManager.clear()`
      - creates the variable `statistics` (of type `Statistics`) holding `entityManager .getEntityManagerFactory() .unwrap(SessionFactory.class) .getStatistics()`
      - calls `statistics.clear()`
      - calls `orders.findById(order.getId())`
      - checks that `statistics.getPrepareStatementCount()` equals `1`
      - calls `Evidence.record( "loadingOneOrderByIdIsExactlyOneStatement: prepareStatementCount = " + statistics.getPrepareStatementCount())`
  - **test `openInViewIsExplicitlyOffInTheApplicationConfiguration`**: reads as "open in view is explicitly off in the application configuration"
    - Signature: `void openInViewIsExplicitlyOffInTheApplicationConfiguration()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - checks that `environment.getProperty("spring.jpa.open-in-view")` equals `"false"`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@DataJpaTest`: starts only the JPA layer (entities, repositories, a database) for a test.
- `@AutoConfigureTestDatabase`: controls whether the test replaces the configured database with an embedded one.
- `@ImportTestcontainers`: starts the containers declared in the named class for this test and wires them into the Spring configuration.
- `@Autowired`: asks Spring to supply the object this field or parameter needs.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.


### `src/test/java/dev/plainround/orders/traps/SchemaMirrorIT.java`

Package `dev.plainround.orders.traps`. sql/001_schema.sql must describe the same schema that Flyway builds from the migrations: the same columns, indexes and constraints.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plainround.orders.support.PostgresContainer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

/**
 * sql/001_schema.sql must describe the same schema that Flyway builds from the migrations: the same
 * columns, indexes and constraints. The migrations ran at startup; the script runs in a second,
 * empty database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportTestcontainers(PostgresContainer.class)
class SchemaMirrorIT {

    private static final String DESCRIBE =
            """
            select 'column ' || table_name || '.' || column_name || ' ' || data_type
                   || ' null=' || is_nullable || ' identity=' || is_identity
                   || ' length=' || coalesce(character_maximum_length::text, '-')
                   || ' default=' || coalesce(column_default, '-')
              from information_schema.columns
             where table_schema = 'public' and table_name <> 'flyway_schema_history'
            union all
            select 'index ' || indexdef from pg_indexes
             where schemaname = 'public' and tablename <> 'flyway_schema_history'
            union all
            select 'constraint ' || conrelid::regclass || ' ' || contype::text || ' ' || pg_get_constraintdef(oid)
              from pg_constraint
             where connamespace = 'public'::regnamespace and conrelid::regclass::text <> 'flyway_schema_history'
            order by 1
            """;

    @Autowired private DataSource flywayDatabase;

    @Test
    void theSchemaScriptBuildsTheSameSchemaAsTheMigrations() throws Exception {
        List<String> fromMigrations;
        try (Connection connection = flywayDatabase.getConnection()) {
            fromMigrations = describe(connection);
            connection.createStatement().execute("CREATE DATABASE schema_script");
        }

        List<String> fromScript;
        try (Connection connection = connectTo("schema_script")) {
            connection.createStatement().execute(Files.readString(Path.of("sql/001_schema.sql")));
            fromScript = describe(connection);
        }

        assertThat(fromMigrations).isNotEmpty();
        assertThat(fromScript).isEqualTo(fromMigrations);
    }

    private static Connection connectTo(String database) throws SQLException {
        String url =
                "jdbc:postgresql://"
                        + PostgresContainer.POSTGRES.getHost()
                        + ":"
                        + PostgresContainer.POSTGRES.getFirstMappedPort()
                        + "/"
                        + database;
        return DriverManager.getConnection(
                url,
                PostgresContainer.POSTGRES.getUsername(),
                PostgresContainer.POSTGRES.getPassword());
    }

    private static List<String> describe(Connection connection) throws SQLException {
        List<String> lines = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(DESCRIBE)) {
            while (rows.next()) {
                lines.add(rows.getString(1));
            }
        }
        return lines;
    }
}
```

Reading it, one piece at a time:

- **`SchemaMirrorIT`**, a class. sql/001_schema.sql must describe the same schema that Flyway builds from the migrations: the same columns, indexes and constraints. The migrations ran at startup; the script runs in a second, empty database.
  - annotation `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)`: starts the whole application inside the test (all beans, the real configuration) so the test can use it
  - annotation `@ImportTestcontainers(PostgresContainer.class)`: starts the containers declared in the named class for this test and wires them into the Spring configuration
  - **field `DESCRIBE`** of type `String`, starting as `""" select 'column ' || table_name || '.' || column_name || ' ' || data_type || ' null=' || is_nullable || ' identity=' || is_identity || ' length=' || coalesce(character_maximum_length::text, '-') || ' default=' || coalesce(column_default, '-') from information_schema.columns where table_schema = 'public' and table_name <> 'flyway_schema_history' union all select 'index ' || indexdef from pg_indexes where schemaname = 'public' and tablename <> 'flyway_schema_history' union all select 'constraint ' || conrelid::regclass || ' ' || contype::text || ' ' || pg_get_constraintdef(oid) from pg_constraint where connamespace = 'public'::regnamespace and conrelid::regclass::text <> 'flyway_schema_history' order by 1 """`
    - Declaration: `private static final String DESCRIBE = """ select 'column ' || table_name || '.' || column_name || ' ' || data_type || ' null=' || is_nullable || ' identity=' || is_identity || ' length=' || coalesce(character_maximum_length::text, '-') || ' default=' || coalesce(column_default, '-') from information_schema.columns where table_schema = 'public' and table_name <> 'flyway_schema_history' union all select 'index ' || indexdef from pg_indexes where schemaname = 'public' and tablename <> 'flyway_schema_history' union all select 'constraint ' || conrelid::regclass || ' ' || contype::text || ' ' || pg_get_constraintdef(oid) from pg_constraint where connamespace = 'public'::regnamespace and conrelid::regclass::text <> 'flyway_schema_history' order by 1 """`
  - **field `flywayDatabase`** of type `DataSource`, set once when the object is built.
    - Declaration: `private DataSource flywayDatabase`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **test `theSchemaScriptBuildsTheSameSchemaAsTheMigrations`**: reads as "the schema script builds the same schema as the migrations"
    - Signature: `void theSchemaScriptBuildsTheSameSchemaAsTheMigrations() throws Exception`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - declares the variable `fromMigrations` of type `List<String>`, to be set later
      - tries this (managing `Connection connection = flywayDatabase.getConnection()` so it is closed afterwards):
        - sets `fromMigrations` to `describe(connection)`
        - calls `connection.createStatement().execute("CREATE DATABASE schema_script")`
      - declares the variable `fromScript` of type `List<String>`, to be set later
      - tries this (managing `Connection connection = connectTo("schema_script")` so it is closed afterwards):
        - calls `connection.createStatement().execute(Files.readString(Path.of("sql/001_schema.sql")))`
        - sets `fromScript` to `describe(connection)`
      - checks that `fromMigrations` is not empty
      - checks that `fromScript` equals `fromMigrations`
  - **method `connectTo`**: the signature is `static Connection connectTo(String database) throws SQLException`.
    - Signature: `private static Connection connectTo(String database) throws SQLException`
    - What the body does, in order:
      - creates the variable `url` (of type `String`) holding `"jdbc:postgresql://" + PostgresContainer.POSTGRES.getHost() + ":" + PostgresContainer.POSTGRES.getFirstMappedPort() + "/" + database`
      - returns `DriverManager.getConnection( url, PostgresContainer.POSTGRES.getUsername(), PostgresContainer.POSTGRES.getPassword())`
  - **method `describe`**: the signature is `static List<String> describe(Connection connection) throws SQLException`.
    - Signature: `private static List<String> describe(Connection connection) throws SQLException`
    - What the body does, in order:
      - creates the variable `lines` (of type `List<String>`) holding `new ArrayList<>()`
      - tries this (managing `Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(DESCRIBE)` so it is closed afterwards):
        - repeats while `rows.next()`:
          - calls `lines.add(rows.getString(1))`
      - returns `lines`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@SpringBootTest`: starts the whole application inside the test (all beans, the real configuration) so the test can use it.
- `@ImportTestcontainers`: starts the containers declared in the named class for this test and wires them into the Spring configuration.
- `@Autowired`: asks Spring to supply the object this field or parameter needs.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **List**: A `List` is an ordered collection of items; `List.of(...)` builds one that cannot be changed afterwards.
- **throws**: `throws X` in a signature says the method may fail with exception `X`; an exception is an object that stops normal flow and travels up the call stack until something catches it.
- **try/catch**: `try { ... } catch (X e) { ... }` runs the first block and, if it throws `X`, runs the second.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.


### `src/test/java/dev/plainround/orders/traps/SelfInvocationIT.java`

Package `dev.plainround.orders.traps`. Trap 2: a `@Transactional` method called from its own class.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.plainround.orders.infrastructure.persistence.CustomerEntity;
import dev.plainround.orders.infrastructure.persistence.CustomerJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderJpaRepository;
import dev.plainround.orders.support.DatabaseReset;
import dev.plainround.orders.support.Evidence;
import dev.plainround.orders.support.PostgresContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Trap 2: a {@code @Transactional} method called from its own class. This class is deliberately not
 * {@code @Transactional}: a test-wide transaction would do the rolling back and hide the bug.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportTestcontainers(PostgresContainer.class)
@Import({SelfInvocationIT.Beans.class})
class SelfInvocationIT {

    static class Beans {
        @Bean
        SelfInvokingCheckout selfInvokingCheckout(
                CustomerJpaRepository customers, OrderJpaRepository orders) {
            return new SelfInvokingCheckout(customers, orders);
        }
    }

    @Autowired private SelfInvokingCheckout checkout;
    @Autowired private CustomerJpaRepository customers;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        DatabaseReset.truncate(jdbc);
    }

    @Test
    void selfInvocationLeavesAPartialOrderOnFailure() {
        Long customerId = customers.save(new CustomerEntity("ada")).getId();

        assertThatThrownBy(() -> checkout.checkoutSelfInvocationBug(customerId, true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(DatabaseReset.count(jdbc, "orders_table")).isEqualTo(1);
        assertThat(DatabaseReset.count(jdbc, "order_lines")).isEqualTo(1);
        Evidence.record(
                "orders after failed self-invocation checkout: "
                        + DatabaseReset.count(jdbc, "orders_table")
                        + "   (a half-built order, persisted with no rollback)");
    }

    @Test
    void proxyFixedRollsBackCompletelyOnTheSameFailure() {
        Long customerId = customers.save(new CustomerEntity("ada")).getId();

        assertThatThrownBy(() -> checkout.checkoutProxyFixed(customerId, true))
                .isInstanceOf(IllegalStateException.class);

        assertThat(DatabaseReset.count(jdbc, "orders_table")).isZero();
        assertThat(DatabaseReset.count(jdbc, "order_lines")).isZero();
        Evidence.record(
                "orders after failed proxy-fixed checkout:      "
                        + DatabaseReset.count(jdbc, "orders_table")
                        + "   (the whole transaction rolled back as one unit)");
    }

    @Test
    void proxyFixedCommitsTheWholeOrderWhenNothingFails() {
        Long customerId = customers.save(new CustomerEntity("ada")).getId();

        checkout.checkoutProxyFixed(customerId, false);

        assertThat(DatabaseReset.count(jdbc, "orders_table")).isEqualTo(1);
        assertThat(DatabaseReset.count(jdbc, "order_lines")).isEqualTo(2);
    }
}
```

Reading it, one piece at a time:

- **`SelfInvocationIT`**, a class. Trap 2: a `@Transactional` method called from its own class. This class is deliberately not `@Transactional`: a test-wide transaction would do the rolling back and hide the bug.
  - annotation `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)`: starts the whole application inside the test (all beans, the real configuration) so the test can use it
  - annotation `@ImportTestcontainers(PostgresContainer.class)`: starts the containers declared in the named class for this test and wires them into the Spring configuration
  - annotation `@Import({SelfInvocationIT.Beans.class})`: adds the named configuration classes to the Spring context
  - **`Beans`**, a class.
    - **method `selfInvokingCheckout`**: the signature is `SelfInvokingCheckout selfInvokingCheckout( CustomerJpaRepository customers, OrderJpaRepository orders)`.
      - Signature: `SelfInvokingCheckout selfInvokingCheckout( CustomerJpaRepository customers, OrderJpaRepository orders)`
      - annotation `@Bean`: tells Spring to call this method once at startup and keep the returned object as a shared, managed object
      - What the body does, in order:
        - returns `new SelfInvokingCheckout(customers, orders)`
  - **field `checkout`** of type `SelfInvokingCheckout`, set once when the object is built.
    - Declaration: `private SelfInvokingCheckout checkout`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `customers`** of type `CustomerJpaRepository`, set once when the object is built.
    - Declaration: `private CustomerJpaRepository customers`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **field `jdbc`** of type `JdbcTemplate`, set once when the object is built.
    - Declaration: `private JdbcTemplate jdbc`
    - annotation `@Autowired`: asks Spring to supply the object this field or parameter needs
  - **method `clean`**: the signature is `void clean()`.
    - Signature: `void clean()`
    - annotation `@AfterEach`: runs this method after every test, to clean up
    - What the body does, in order:
      - calls `DatabaseReset.truncate(jdbc)`
  - **test `selfInvocationLeavesAPartialOrderOnFailure`**: reads as "self invocation leaves a partial order on failure"
    - Signature: `void selfInvocationLeavesAPartialOrderOnFailure()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customerId` (of type `Long`) holding `customers.save(new CustomerEntity("ada")).getId()`
      - checks that running `checkout.checkoutSelfInvocationBug(customerId, true)` fails
        - the failure is an instance of `IllegalStateException.class`
      - checks that `DatabaseReset.count(jdbc, "orders_table")` equals `1`
      - checks that `DatabaseReset.count(jdbc, "order_lines")` equals `1`
      - calls `Evidence.record( "orders after failed self-invocation checkout: " + DatabaseReset.count(jdbc, "orders_table") + " (a half-built order, persisted with no rollback)")`
  - **test `proxyFixedRollsBackCompletelyOnTheSameFailure`**: reads as "proxy fixed rolls back completely on the same failure"
    - Signature: `void proxyFixedRollsBackCompletelyOnTheSameFailure()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customerId` (of type `Long`) holding `customers.save(new CustomerEntity("ada")).getId()`
      - checks that running `checkout.checkoutProxyFixed(customerId, true)` fails
        - the failure is an instance of `IllegalStateException.class`
      - checks that `DatabaseReset.count(jdbc, "orders_table")` is zero
      - checks that `DatabaseReset.count(jdbc, "order_lines")` is zero
      - calls `Evidence.record( "orders after failed proxy-fixed checkout: " + DatabaseReset.count(jdbc, "orders_table") + " (the whole transaction rolled back as one unit)")`
  - **test `proxyFixedCommitsTheWholeOrderWhenNothingFails`**: reads as "proxy fixed commits the whole order when nothing fails"
    - Signature: `void proxyFixedCommitsTheWholeOrderWhenNothingFails()`
    - annotation `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws
    - What the body does, in order:
      - creates the variable `customerId` (of type `Long`) holding `customers.save(new CustomerEntity("ada")).getId()`
      - calls `checkout.checkoutProxyFixed(customerId, false)`
      - checks that `DatabaseReset.count(jdbc, "orders_table")` equals `1`
      - checks that `DatabaseReset.count(jdbc, "order_lines")` equals `2`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@SpringBootTest`: starts the whole application inside the test (all beans, the real configuration) so the test can use it.
- `@ImportTestcontainers`: starts the containers declared in the named class for this test and wires them into the Spring configuration.
- `@Import`: adds the named configuration classes to the Spring context.
- `@Bean`: tells Spring to call this method once at startup and keep the returned object as a shared, managed object.
- `@Autowired`: asks Spring to supply the object this field or parameter needs.
- `@AfterEach`: runs this method after every test, to clean up.
- `@Test`: marks a method as one test; the test runner calls it and the test passes unless something inside throws.

Language features in this file, explained:

- **static**: `static` means the member belongs to the class itself, not to one object; you use it without creating an object.
- **private**: `private` hides a member from every other class.
- **lambda (`->`)**: An arrow `a -> b` is a lambda: a small unnamed function. The part before the arrow lists its inputs and the part after is what it does.
- **AssertJ `assertThat`**: `assertThat(actual).isXxx(...)` is AssertJ: it checks the actual value and throws an assertion error, which fails the test, when the check is false.
- **`assertThatThrownBy`**: `assertThatThrownBy(() -> code)` runs the code and checks that it throws; the chained calls describe the exception expected.


### `src/test/java/dev/plainround/orders/traps/SelfInvokingCheckout.java`

Package `dev.plainround.orders.traps`. Trap 2, planted: the code a reviewer sees in a pull request, and its fix beside it.

The whole file, as it is in the repository:

```java
package dev.plainround.orders.traps;

import dev.plainround.orders.infrastructure.persistence.CustomerEntity;
import dev.plainround.orders.infrastructure.persistence.CustomerJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderEntity;
import dev.plainround.orders.infrastructure.persistence.OrderJpaRepository;
import dev.plainround.orders.infrastructure.persistence.OrderLineEntity;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trap 2, planted: the code a reviewer sees in a pull request, and its fix beside it. Test code
 * only.
 */
class SelfInvokingCheckout {

    private final CustomerJpaRepository customers;
    private final OrderJpaRepository orders;

    SelfInvokingCheckout(CustomerJpaRepository customers, OrderJpaRepository orders) {
        this.customers = customers;
        this.orders = orders;
    }

    /**
     * THE BUG: no annotation here, and the call below is a same-object call that skips the proxy.
     */
    public void checkoutSelfInvocationBug(Long customerId, boolean failHalfway) {
        this.saveOrder(customerId, failHalfway);
    }

    /** Looks transactional, but is never wrapped when called from the method above. */
    @Transactional
    public void saveOrder(Long customerId, boolean failHalfway) {
        placeInTwoSteps(customerId, failHalfway);
    }

    /**
     * THE FIX: the public entry point carries the annotation, so every call gets one real
     * transaction.
     */
    @Transactional
    public void checkoutProxyFixed(Long customerId, boolean failHalfway) {
        placeInTwoSteps(customerId, failHalfway);
    }

    private void placeInTwoSteps(Long customerId, boolean failHalfway) {
        CustomerEntity customer = customers.findById(customerId).orElseThrow();
        OrderEntity order = new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"));
        order.setCustomer(customer);
        orders.save(order);
        order.addLine(new OrderLineEntity("sku-1", 1));
        orders.save(order);
        if (failHalfway) {
            throw new IllegalStateException("simulated failure partway through checkout");
        }
        order.addLine(new OrderLineEntity("sku-2", 1));
        orders.save(order);
    }
}
```

Reading it, one piece at a time:

- **`SelfInvokingCheckout`**, a class. Trap 2, planted: the code a reviewer sees in a pull request, and its fix beside it. Test code only.
  - **field `customers`** of type `CustomerJpaRepository`, set once when the object is built.
    - Declaration: `private final CustomerJpaRepository customers`
  - **field `orders`** of type `OrderJpaRepository`, set once when the object is built.
    - Declaration: `private final OrderJpaRepository orders`
  - **constructor `SelfInvokingCheckout`**: the signature is `SelfInvokingCheckout(CustomerJpaRepository customers, OrderJpaRepository orders)`.
    - Signature: `SelfInvokingCheckout(CustomerJpaRepository customers, OrderJpaRepository orders)`
    - What the body does, in order:
      - stores `customers` in the field `customers`
      - stores `orders` in the field `orders`
  - **method `checkoutSelfInvocationBug`**: THE BUG: no annotation here, and the call below is a same-object call that skips the proxy.
    - Signature: `public void checkoutSelfInvocationBug(Long customerId, boolean failHalfway)`
    - What the body does, in order:
      - calls `this.saveOrder(customerId, failHalfway)`
  - **method `saveOrder`**: Looks transactional, but is never wrapped when called from the method above.
    - Signature: `public void saveOrder(Long customerId, boolean failHalfway)`
    - annotation `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
    - What the body does, in order:
      - calls `placeInTwoSteps(customerId, failHalfway)`
  - **method `checkoutProxyFixed`**: THE FIX: the public entry point carries the annotation, so every call gets one real transaction.
    - Signature: `public void checkoutProxyFixed(Long customerId, boolean failHalfway)`
    - annotation `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back)
    - What the body does, in order:
      - calls `placeInTwoSteps(customerId, failHalfway)`
  - **method `placeInTwoSteps`**: the signature is `void placeInTwoSteps(Long customerId, boolean failHalfway)`.
    - Signature: `private void placeInTwoSteps(Long customerId, boolean failHalfway)`
    - What the body does, in order:
      - creates the variable `customer` (of type `CustomerEntity`) holding `customers.findById(customerId).orElseThrow()`
      - creates the variable `order` (of type `OrderEntity`) holding `new OrderEntity(Instant.parse("2026-10-27T10:00:00Z"))`
      - calls `order.setCustomer(customer)`
      - calls `orders.save(order)`
      - calls `order.addLine(new OrderLineEntity("sku-1", 1))`
      - calls `orders.save(order)`
      - when `failHalfway` is true:
        - throws a new `IllegalStateException` created with `"simulated failure partway through checkout"`
      - calls `order.addLine(new OrderLineEntity("sku-2", 1))`
      - calls `orders.save(order)`

Annotations in this file (an annotation is a label starting with `@` that tools read):

- `@Transactional`: wraps the call in a database transaction (a group of changes that all succeed or all roll back).

Language features in this file, explained:

- **final**: `final` on a field or variable means it is assigned once and never changed; on a class it forbids subclasses.
- **private**: `private` hides a member from every other class.
- **public**: `public` lets any class use the member.


## Try it yourself

1. Run one test class: `./mvnw -q test -Dtest=BookTest` (use any class name from above).
2. Break one expected value in a test, run it, and read the failure message. Then undo your edit.
3. Run `./mvnw verify` to run everything, including the integration tests and the coverage check.

## Mistakes people make

- Writing a test that cannot fail. Always see it fail once.
- Sharing state between tests, so that one test passes only when another ran first. Every test here builds its own data.
- Asserting on implementation details instead of behavior.

## Check yourself

1. How does a test fail? *By throwing, usually from an assertion.*
2. What do `*Test` and `*IT` mean? *Unit tests run by Surefire; integration tests run by Failsafe.*
3. Why are test names long sentences? *The failure report shows only the name.*

## Further reading

[JUnit](junit.md), [AssertJ](assertj.md), [Mockito](mockito.md), [Spring Boot Test](spring-boot-test.md) and [ArchUnit](archunit.md) in this folder.
