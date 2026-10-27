# Spring Boot 4.1.2

## What it is

Spring Boot is a layer on top of Spring Framework that removes setup work. It gives you three things: **starters** (one dependency that brings a whole feature), **auto-configuration** (it looks at which libraries are on the classpath and creates the beans you would otherwise write by hand), and a **runnable jar** (one file with an embedded web server inside, started with `java -jar`).

## Why this repository uses it

The lesson says to start from a Spring Boot project with a few starters and "nothing else". Boot supplies the web server, JSON handling, validation, health checks and logging with almost no code, so the project's own code is only the lesson.

## Where it appears

| Path | What it does |
| --- | --- |
| `OrderServiceApplication.java` | the entry point, marked `@SpringBootApplication` |
| `pom.xml` | the Boot parent and the starters |
| `application.properties` | the settings Boot reads |
| `interfaces/web/*Properties.java` or `infrastructure/*Properties.java` | records bound to settings with `@ConfigurationProperties` |
| `Dockerfile` | uses Boot's `-Djarmode=tools extract --layers` to split the jar |

## How it works underneath

1. `main` calls `SpringApplication.run(...)`. Boot builds the application context, starts the embedded **Tomcat** web server on port 8080 (or `server.port`), and keeps the process alive.
2. `@SpringBootApplication` is three annotations in one: it marks a configuration class, turns on **auto-configuration**, and turns on component scanning of the package and below.
3. Auto-configuration is a list of configuration classes shipped inside Boot's jars. Each is guarded by **conditions** such as "only if this class exists" or "only if no bean of this type exists yet". When you add the web starter, the Tomcat and MVC configuration classes switch on; if you define your own bean of the same type, Boot's backs off.
4. `./mvnw spring-boot:run` runs the application from the source tree; `mvn package` makes the runnable jar (the plugin "repackages" the ordinary jar with its libraries inside).
5. When the process receives SIGTERM (the polite stop signal), Boot runs a **graceful shutdown** if asked to: it stops accepting new requests, waits for running ones, then exits.

## Walkthrough of this repository's use

The entry point:

```java
@SpringBootApplication
public class OrderServiceApplication {

    /**
     * Starts the application.
     *
     * @param args command-line arguments, passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

`@SpringBootApplication` does the three things above.  `SpringApplication.run(OrderServiceApplication.class, args)` starts everything; `args` lets you pass `--server.port=...` on the command line.

**Starters** (from the POM):

```xml
  <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
```

`webmvc` brings Tomcat, Spring MVC and Jackson; `validation` brings Bean Validation; `actuator` brings the health and metrics endpoints. `data-jpa` brings Hibernate and Spring Data; `flyway` brings the migration tool.

**Settings as typed objects.**

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
```

This project has no `@ConfigurationProperties` record; its settings are read directly by Spring Data and Hikari from `application.properties`. The block above is the filter that Boot registers automatically because it is a `@Component` that implements a servlet `Filter`: Boot finds such beans and adds them to the web server's filter chain.

**Graceful shutdown and probes** are two lines in `application.properties`: `server.shutdown=graceful` and `management.endpoint.health.probes.enabled=true`.

## Try it yourself

1. `./mvnw spring-boot:run` starts the application from source.
2. `./mvnw -DskipTests package && java -jar target/spring-order-service-traps-0.1.0.jar` runs the packaged jar. `unzip -l` on that file shows `BOOT-INF/lib/` with the libraries inside.
3. Start with `--debug` (`java -jar ... --debug`) and search the log for `CONDITIONS EVALUATION REPORT`: it lists every auto-configuration that matched and why.
4. Stop the process with Ctrl+C and read the last log lines: `Commencing graceful shutdown` then `Graceful shutdown complete`.

## Mistakes people make

- Putting the main class outside the package that holds the other classes: component scanning then misses them.
- Adding a library and expecting no effect: auto-configuration may switch on a feature (a datasource, security).
- Editing `application.properties` and not restarting.
- Using `@Autowired` fields instead of the constructor (this repository forbids it with a test).

## Check yourself

1. What is a starter? *One dependency that brings a feature's libraries.*
2. What makes Boot "back off" from a bean? *A bean of that type that you define.*
3. What does `@SpringBootApplication` combine? *Configuration, auto-configuration, component scan.*
4. What does graceful shutdown wait for? *Requests already running.*
5. Why validate a `@ConfigurationProperties` record? *A bad setting fails at startup.*

## Further reading

- Spring Boot 4.1.2 reference: https://docs.spring.io/spring-boot/4.1.2/reference/index.html
- Auto-configuration: https://docs.spring.io/spring-boot/4.1.2/reference/using/auto-configuration.html
- Graceful shutdown: https://docs.spring.io/spring-boot/4.1.2/reference/web/graceful-shutdown.html
