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

1. At startup Spring Boot collects settings from several **sources** and ranks them. From strongest to weakest, the ones that matter here: command-line arguments (`--server.port=0`), operating-system environment variables, then the properties files.
2. **Relaxed binding:** the environment variable `READING_LIST_MAX_PAGE_SIZE` and the property `reading-list.max-page-size` name the same setting. Upper case with underscores works for any key. That is how a container is configured without touching the file.
3. **Placeholders:** `${NAME:default}` means "the value of NAME if it is set, otherwise `default`". `${NAME}` with no default stays unresolved if `NAME` is missing.
4. Two files with the same name can both apply. Spring Boot reads `classpath:/application.properties` and `classpath:/config/application.properties` and the second wins. That is why the test overrides live in a `config/` folder: a test file placed beside the main one would **replace** it (the same classpath path), and the app would lose its settings.

## Walkthrough of this repository's use

The whole file:

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

- `spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:15432}/${POSTGRES_DB:orders}?connectTimeout=3&socketTimeout=30&tcpKeepAlive=true` builds the database address from placeholders with defaults. After the `?` are options for the PostgreSQL driver: give up connecting after 3 seconds, give up waiting for an answer after 30, send keep-alive packets.
- `spring.datasource.password=${POSTGRES_PASSWORD}` has no default on purpose: the password only ever comes from the environment.
- `spring.datasource.hikari.*` configures the **connection pool** (a set of open database connections that requests borrow): at most 10, wait at most 3000 ms for one, retry the first connection for up to 30 seconds at startup.
- `spring.transaction.default-timeout=15s` and `spring.jpa.properties.jakarta.persistence.query.timeout=10000` cap how long a transaction and a query may run.
- `spring.jpa.open-in-view=false` is the lesson's rule from the first commit (see `spring-data-jpa.md`). `spring.jpa.hibernate.ddl-auto=validate` makes Hibernate check, never change, the schema.
- `logging.structured.*`, `management.*` and `server.shutdown` are the same operability settings as in the other projects.

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
