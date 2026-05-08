package org.saltations.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extracts Freemarker variable names from an archetype directory tree.
 * Scans file content and file/folder name path segments.
 * Per D-04: captures top-level name only (${user.email} -> "user", ${title?upper_case} -> "title").
 * Per D-05: throws IOException if any file contains an unclosed ${ expression.
 * Skips hidden files and directories (names starting with ".").
 * Returns sorted, deduplicated list for stable YAML output.
 */
public class FreemarkerVariableExtractor {

    // D-04: matches ${identifier} capturing the top-level name before any . or ?
    private static final Pattern VAR_PATTERN =
        Pattern.compile("\\$\\{([a-zA-Z_$][a-zA-Z0-9_$]*)(?:[.?][^}]*)?\\}");

    // D-05: detects unclosed ${ — ${ not followed by any } before end of line
    private static final Pattern UNCLOSED_PATTERN =
        Pattern.compile("\\$\\{(?![^}]*\\})");

    /**
     * Walks the given directory, extracting Freemarker variable names from path segments
     * and file content.
     *
     * @param root the archetype directory root
     * @return sorted, deduplicated list of variable names
     * @throws IOException if a file cannot be read or contains a malformed expression
     */
    public List<String> extractFromDirectory(Path root) throws IOException {
        Set<String> vars = new LinkedHashSet<>();

        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> paths = stream
                .filter(p -> !HiddenPathFilter.hasHiddenPathSegment(root, p))
                .collect(Collectors.toList());

            for (Path p : paths) {
                // Extract from path segment name (folder names and filenames)
                extractFromString(p.getFileName().toString(), vars);

                // Extract from file content
                if (Files.isRegularFile(p)) {
                    String content = Files.readString(p, StandardCharsets.UTF_8);
                    checkForMalformed(content, p);
                    extractFromString(content, vars);
                }
            }
        }

        List<String> sorted = new ArrayList<>(vars);
        Collections.sort(sorted);
        return sorted;
    }

    private void extractFromString(String text, Set<String> vars) {
        Matcher m = VAR_PATTERN.matcher(text);
        while (m.find()) {
            vars.add(m.group(1));
        }
    }

    private void checkForMalformed(String content, Path file) throws IOException {
        if (UNCLOSED_PATTERN.matcher(content).find()) {
            throw new IOException(
                "Malformed Freemarker expression in: " + file.toAbsolutePath()
                + " (unclosed ${)");
        }
    }
}
