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
