# Testcontainers

## What it is

Testcontainers is a Java library that starts Docker containers from inside a test, waits until they are ready, hands the test their connection details, and removes them afterwards.

## Why this repository uses it

The lesson insists the tests run against PostgreSQL, not an in-memory database, because statement counts, locking and SQL dialect differ. Testcontainers gives every machine and CI the same real database without installing anything but Docker.

## Where it appears

| Path | Role |
| --- | --- |
| `src/test/java/dev/plainround/orders/support/PostgresContainer.java` | defines the container once for all integration tests |
| every `*IT` class | imports it with `@ImportTestcontainers(PostgresContainer.class)` |
| `pom.xml` | `testcontainers-junit-jupiter`, `testcontainers-postgresql`, `spring-boot-testcontainers` |

## How it works underneath

1. The test JVM talks to the Docker engine (the same one `docker` uses). It starts a helper container called **Ryuk** whose job is to remove the test containers if the JVM dies.
2. It creates and starts `postgres:18`, publishing the container's port 5432 on a **random** free host port, so tests never clash with another database.
3. The **wait strategy** watches the container's log until the line "database system is ready to accept connections" has appeared twice (PostgreSQL prints it once after initialization, then again after the restart it does at the end of init).
4. `@ServiceConnection` tells Spring Boot: this container is the database; set the datasource URL, user and password from it. No `spring.datasource.*` properties are needed in tests.
5. A container declared as a `static` field is started once and shared by all test classes in the JVM, which saves many seconds.

## Walkthrough of this repository's use

```java
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

- `@Container` marks the field as a container to manage. `@ServiceConnection` wires it into Spring Boot.
- `public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18")` creates the container description. In Testcontainers 2.x the class lives in `org.testcontainers.postgresql`.
- `.waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2).withStartupTimeout(Duration.ofMinutes(5)))` replaces the default wait (60 seconds) with a five-minute one: the default timed out on a heavily loaded machine. `2` is how many times the line must appear.

How a test uses it:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ImportTestcontainers(PostgresContainer.class)
class CheckoutIT {
```

`@ImportTestcontainers(PostgresContainer.class)` imports the static container fields as beans: Spring starts the database before the context and connects to it. The test then runs against a real PostgreSQL 18 with the real Flyway migrations.

`DatabaseReset.truncate(jdbc)` (in `support/`) empties all tables with `TRUNCATE ... RESTART IDENTITY CASCADE` between tests that commit real transactions.

## Try it yourself

1. `docker info` must work; then `./mvnw verify -Dit.test=OrderModelIT -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false` starts the container and runs one integration test.
2. While it runs, `docker ps` in another terminal shows `postgres:18` on a random port and a `testcontainers/ryuk` container.
3. When the build ends, `docker ps` shows both gone.

## Mistakes people make

- Running the tests without Docker: they fail at startup with "Could not find a valid Docker environment".
- Using a fixed host port: tests collide.
- Starting a new container per test class: slow.
- Relying on an in-memory database that differs from production.
- On a very slow machine, the default 60-second wait fails; this repository sets five minutes.

## Check yourself

1. What does Testcontainers need on your machine? *Docker.*
2. Why a random host port? *To avoid clashes with other databases.*
3. What does `@ServiceConnection` do? *Points Spring's datasource at the container.*
4. What is Ryuk for? *Removing containers if the test JVM dies.*
5. Why a static container? *It is shared by all tests in the JVM.*

## Further reading

- Testcontainers for Java (2.0.x): https://java.testcontainers.org/
- Spring Boot 4.1.2, Testcontainers support: https://docs.spring.io/spring-boot/4.1.2/reference/testing/testcontainers.html
