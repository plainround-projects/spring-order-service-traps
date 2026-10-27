# Technology notes for spring-order-service-traps

This folder teaches every technology the repository uses, from zero, through the repository's own files. Each note says what the technology is, why this repository uses it, where it appears, how it works, and then walks through the repository's own lines. You need no earlier knowledge.

## The files and the order to read them

Read from the top: the language first, then the build tool, then the frameworks, then the runtime pieces, then the tests, then the tools around the code.

| # | Note | Role in this repository |
| ---: | --- | --- |
| 1 | [Java 25](java.md) | the language every class, test and build plugin is written in |
| 2 | [Maven and the Maven wrapper](maven.md) | builds, tests and packages the code; `./mvnw` is the only build command you need |
| 3 | [XML](xml.md) | the file format of `pom.xml` and of the Checkstyle rules |
| 4 | [Properties files (application.properties)](properties-files.md) | the one file of settings that Spring Boot reads at startup |
| 5 | [Spring Framework (beans, injection, proxies)](spring-framework.md) | builds the objects, hands each one what it needs, and wraps some of them with extra behavior |
| 6 | [Spring Boot 4.1.2](spring-boot.md) | starts the application, configures it from the libraries on the classpath, and provides the starters |
| 7 | [Spring Web MVC (controllers, filters, error advice)](spring-web-mvc.md) | turns HTTP requests into method calls and results into HTTP answers |
| 8 | [Bean Validation (Jakarta Validation)](bean-validation.md) | checks input at the boundary with annotations such as `@NotBlank` and `@Min` |
| 9 | [Problem Details (RFC 9457, application/problem+json)](problem-details.md) | the one JSON shape every error answer in this service uses |
| 10 | [JSON and Jackson](json-jackson.md) | the text format of every request and answer, and the library that converts it to and from Java records |
| 11 | [Spring Boot Actuator (health, probes, metrics endpoints)](spring-boot-actuator.md) | the endpoints that say whether the service is alive and ready |
| 12 | [Logging (SLF4J, Logback, structured JSON)](structured-logging.md) | writes one JSON object per log line, with a request id |
| 13 | [JUnit (Jupiter)](junit.md) | runs every test method and reports which passed |
| 14 | [PostgreSQL 18](postgresql.md) | the relational database that stores customers, orders, order lines, stock and the audit trail |
| 15 | [AssertJ](assertj.md) | the readable assertions (`assertThat(x).isEqualTo(y)`) used in every test |
| 16 | [SQL](sql.md) | the language of the schema (DDL), the seed data (DML) and the lesson's queries |
| 17 | [Flyway (database migrations)](flyway.md) | applies the schema files in order at startup and records which have run |
| 18 | [Mockito](mockito.md) | creates stand-in objects ("mocks") for the collaborators a test does not want to run |
| 19 | [Spring Boot Test (test slices, MockMvc, full-context tests)](spring-boot-test.md) | starts as much of the application as a test needs, from the web layer only to the whole service |
| 20 | [JPA, Hibernate and Spring Data JPA](spring-data-jpa.md) | maps Java objects to the tables, and reads and writes them without hand-written SQL |
| 21 | [ArchUnit](archunit.md) | turns the dependency rule between layers into a test that fails the build |
| 22 | [Spring transactions (@Transactional, proxies, REQUIRES_NEW)](spring-transactions.md) | the transaction boundary of the checkout and the two ways it goes wrong in the lesson |
| 23 | [JaCoCo (code coverage)](jacoco.md) | measures which lines the tests ran and fails the build below a limit |
| 24 | [Testcontainers](testcontainers.md) | starts a real PostgreSQL 18 in a container for the integration tests |
| 25 | [Spotless and google-java-format](spotless-google-java-format.md) | formats every Java file the same way, and fails the build if a file is not formatted |
| 26 | [Checkstyle](checkstyle.md) | enforces the written rules a formatter cannot: names, imports, public Javadoc, no leftovers |
| 27 | [EditorConfig](editorconfig.md) | tells every editor the same indentation, line ending and charset for each file type |
| 28 | [Git and .gitignore](git-and-gitignore.md) | keeps the history of the repository and decides which files are never recorded |
| 29 | [Dockerfile (container images)](dockerfile.md) | builds the image that runs the service: a multi-stage build ending in a small, non-root runtime |
| 30 | [Docker Compose](docker-compose.md) | starts the service, its database together with one command |
| 31 | [YAML](yaml.md) | the file format of `docker-compose.yml` and the GitHub Actions workflow |
| 32 | [Make and the Makefile](make.md) | gives every task a short name: `make up`, `make run`, `make test`, `make lint`, `make down` |
| 33 | [Bash (the scripts)](bash.md) | the scripts under `scripts/` that run the lesson's scenario |
| 34 | [curl](curl.md) | sends HTTP requests from the command line; the scripts and README examples use it |
| 35 | [jq](jq.md) | reads and reshapes JSON on the command line |
| 36 | [GitHub Actions](github-actions.md) | runs lint, tests, the dependency audit and the image build on every push |
| 37 | [OSV-Scanner (dependency audit)](osv-scanner.md) | checks every library the project uses against a database of known vulnerabilities |

## If you have ten minutes

Read these three, in this order:

1. [JPA, Hibernate and Spring Data JPA](spring-data-jpa.md): maps Java objects to the tables, and reads and writes them without hand-written SQL
1. [Spring transactions (@Transactional, proxies, REQUIRES_NEW)](spring-transactions.md): the transaction boundary of the checkout and the two ways it goes wrong in the lesson
1. [Testcontainers](testcontainers.md): starts a real PostgreSQL 18 in a container for the integration tests
