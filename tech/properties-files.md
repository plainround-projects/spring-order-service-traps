# Properties files (application.properties)

## What it is

A properties file is a plain text file of `key=value` lines. A line that starts with `#` is a comment. Spring Boot reads a file named `application.properties` from the classpath (the places Java looks for code and resources; here `src/main/resources`) and turns each line into a setting.

## Why this repository uses it

The lesson wants configuration to come from the environment with checked defaults, never hard-coded in code. This file holds every setting that is not code: ports, timeouts, limits, logging style, shutdown behavior.

## Where it appears

| Path | What it does |
| --- | --- |
| `src/main/resources/application.properties` | the service's settings, including the database connection |
| `src/test/resources/config/application.properties` | test-only changes: Hibernate statistics, a longer transaction timeout, a bigger connection pool |
| `.mvn/jvm.config` | a different thing with a similar look: one JVM option per line |

## How it works underneath

First, three words this note uses:

- An **environment variable** is a named value that the operating system gives to every program it starts, for example `POSTGRES_PASSWORD=abc`. A program reads it by name. Containers and scripts use them to pass settings in without changing any file.
- A **port** is a number (0 to 65535) that tells the operating system which program on this machine should receive a network connection. A web server listens on one port; a client says "host plus port" to reach it.
- The **classpath** is the list of places where Java looks for classes and resource files while the program runs. Maven copies everything under `src/main/resources` into `target/classes`, which is on the classpath.

How the file is used:

1. At startup Spring Boot collects settings from several **sources** and ranks them. From strongest to weakest, the ones that matter here: command-line arguments (`--server.port=0`), then environment variables, then the properties files.
2. **Relaxed binding:** the environment variable `READING_LIST_MAX_PAGE_SIZE` and the property `reading-list.max-page-size` name the same setting. Upper case with underscores works for any key. That is how a container is configured without touching the file.
3. **Placeholders:** `${NAME:default}` means "the value of NAME (an environment variable or another setting) if it is set, otherwise `default`". `${NAME}` with no default stays unresolved if `NAME` is missing.
4. Two files with the same name can both apply. Spring Boot reads `classpath:/application.properties` and `classpath:/config/application.properties`, and the second wins. That is why the test overrides live in a `config/` folder: a test file placed beside the main one would **replace** it (the same classpath path), and the application would lose its settings.
5. A line that starts with `#` is a comment. Everything after the first `=` is the value, including spaces.

## Walkthrough of this repository's use

The whole main file:

```properties
# Database: every value comes from the environment (see .env.example). The password has no default on purpose:
# a missing password stops the application at startup instead of connecting with a guessable one.
spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:15432}/${POSTGRES_DB:orders}?connectTimeout=3&socketTimeout=30&tcpKeepAlive=true
spring.datasource.username=${POSTGRES_USER:orders}
spring.datasource.password=${POSTGRES_PASSWORD}

# Pool and timeouts: waiting for a connection, and for the database at startup, are bounded.
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.connection-timeout=3000
spring.datasource.hikari.initialization-fail-timeout=30000
spring.transaction.default-timeout=15s
spring.jpa.properties.jakarta.persistence.query.timeout=10000

# The lesson's rule from the first commit: no session held open for the whole request, so a lazy load outside a
# transaction fails loudly instead of working by accident. Hibernate checks the schema; Flyway owns it.
spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.jdbc.time_zone=UTC

spring.mvc.problemdetails.enabled=true

# Operability: JSON logs, probes, and in-flight requests finish before the process stops.
logging.structured.format.console=logstash
logging.structured.json.add.service=order-service
management.endpoints.web.exposure.include=health
management.endpoint.health.probes.enabled=true
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=20s
```

Every key in that file, with the value it has here and what it does:

| Key | Value in this file | What it does |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:15432}/${POSTGRES_DB:orders}?connectTimeout=3&socketTimeout=30&tcpKeepAlive=true` | The JDBC address of the database (host, port, database name) plus driver options after the `?`. The `${NAME:default}` parts are filled from environment variables. |
| `spring.datasource.username` | `${POSTGRES_USER:orders}` | The database user, from `POSTGRES_USER` with a default. |
| `spring.datasource.password` | `${POSTGRES_PASSWORD}` | The database password. It has no default: it must come from the environment variable `POSTGRES_PASSWORD`. |
| `spring.datasource.hikari.maximum-pool-size` | `10` | The most open database connections the pool keeps. |
| `spring.datasource.hikari.connection-timeout` | `3000` | How long a request may wait to borrow a database connection from the pool, in milliseconds. |
| `spring.datasource.hikari.initialization-fail-timeout` | `30000` | At startup, how long the pool keeps trying to reach the database before giving up, in milliseconds. |
| `spring.transaction.default-timeout` | `15s` | The longest a transaction may run before it is rolled back. |
| `spring.jpa.properties.jakarta.persistence.query.timeout` | `10000` | Cancel any query that runs longer than this many milliseconds. |
| `spring.jpa.open-in-view` | `false` | `false` closes the database session when the service method ends, so touching a lazy association later (while the web layer writes the answer) fails visibly. |
| `spring.jpa.hibernate.ddl-auto` | `validate` | `validate` makes Hibernate only check that the tables match the entity classes; it never creates or changes tables (Flyway does that). |
| `spring.jpa.properties.hibernate.jdbc.time_zone` | `UTC` | Store and read timestamps in UTC. |
| `spring.mvc.problemdetails.enabled` | `true` | Turn on `application/problem+json` answers for the framework's own errors. |
| `logging.structured.format.console` | `logstash` | Print each log line to the console as one JSON object (the "logstash" layout) instead of plain text. Tools can then search by field. |
| `logging.structured.json.add.service` | `order-service` | Add a fixed field named `service` to every JSON log line, so lines from different programs can be told apart. |
| `management.endpoints.web.exposure.include` | `health` | Which Actuator endpoints are reachable over HTTP. An endpoint is a built-in address under `/actuator`; by default only `health` is reachable, and this list adds the others. |
| `management.endpoint.health.probes.enabled` | `true` | Turn on two extra health addresses, `/actuator/health/liveness` ("the process is alive") and `/actuator/health/readiness` ("it can take requests now"). |
| `server.shutdown` | `graceful` | `graceful` means: when told to stop, stop accepting new requests, let the running ones finish, then exit. |
| `spring.lifecycle.timeout-per-shutdown-phase` | `20s` | The longest the graceful shutdown waits for running requests. |

The comment lines in the file explain the reason for each group of settings; they are part of the file and worth reading in place.

The test file, which Spring Boot merges over the main one when tests run:

```properties
# Test-only overrides, merged over src/main/resources/application.properties.
# Plain-text logs for unit and slice tests; the end-to-end test turns JSON on itself.
logging.structured.format.console=
# The traps are measured by counting SQL statements, so Hibernate keeps statistics in tests (never in production).
spring.jpa.properties.hibernate.generate_statistics=true
# 20 connections: each of 8 racing checkouts holds one and the REQUIRES_NEW audit needs a second (ADR 0002).
# The test transaction of @DataJpaTest seeds hundreds of rows; the production timeout (15 s) must not fail a slow CI machine.
spring.transaction.default-timeout=300s
spring.datasource.hikari.maximum-pool-size=20
```

| Key | Value in this file | What it does |
| --- | --- | --- |
| `logging.structured.format.console` | `(empty)` | Print each log line to the console as one JSON object (the "logstash" layout) instead of plain text. Tools can then search by field. |
| `spring.jpa.properties.hibernate.generate_statistics` | `true` | Make Hibernate count what it does (statements prepared, entities loaded). The tests read these counts. |
| `spring.transaction.default-timeout` | `300s` | The longest a transaction may run before it is rolled back. |
| `spring.datasource.hikari.maximum-pool-size` | `20` | The most open database connections the pool keeps. |

One more file has a similar look but is not a properties file for Spring: `.mvn/jvm.config`. It holds one JVM option per line, and Maven passes them to the Java process that runs Maven itself (see `maven.md`).

## Try it yourself

1. Start the application with a different port without editing the file: `SERVER_PORT=18999 ./mvnw spring-boot:run` and look for `Tomcat started on port 18999` in the log.
2. Override with an argument: add `-Dspring-boot.run.arguments=--server.port=18998`.
3. Break a validated value and watch startup fail: `POSTGRES_PORT=1 ./mvnw spring-boot:run` waits up to 30 seconds for the database (the pool's startup retry) and then fails with a connection error.
4. `grep -v '^#' src/main/resources/application.properties` (from the module folder) shows the settings without the comments.

## Mistakes people make

- Putting test settings in `src/test/resources/application.properties`: it hides the main file. Use `config/application.properties`.
- Writing spaces around `=`: they become part of the value in some tools; this repository writes none.
- Expecting a changed file to apply without a rebuild: the file is copied to `target/classes` at build time.
- Putting a secret in the file: here, secrets come from environment variables only.

## Check yourself

1. What does `${A:b}` mean? *The value of `A`, or `b` if `A` is not set.*
2. Which wins, an environment variable or the file? *The environment variable.*
3. Why is the test file in a `config/` folder? *So it adds to the main file instead of replacing it.*
4. How can `READING_LIST_MAX_PAGE_SIZE` set `reading-list.max-page-size`? *Relaxed binding maps the two names.*
5. What happens to a `#` line? *It is a comment and ignored.*

## Further reading

- Spring Boot 4.1.2, "Externalized Configuration": https://docs.spring.io/spring-boot/4.1.2/reference/features/external-config.html
- Common application properties: https://docs.spring.io/spring-boot/4.1.2/appendix/application-properties/index.html
