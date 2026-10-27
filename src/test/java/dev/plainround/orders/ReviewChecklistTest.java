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
