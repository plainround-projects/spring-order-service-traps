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
