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
