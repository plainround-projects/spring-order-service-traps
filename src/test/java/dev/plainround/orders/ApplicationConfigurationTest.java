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
