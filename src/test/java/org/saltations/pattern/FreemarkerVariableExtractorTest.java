package org.saltations.pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FreemarkerVariableExtractorTest {

    @TempDir Path tempDir;

    private final FreemarkerVariableExtractor extractor = new FreemarkerVariableExtractor();

    @Test
    void extractsSimpleVariablesFromFileContent() throws Exception {
        Path f = tempDir.resolve("template.txt");
        Files.writeString(f, "Hello ${name}, your email is ${email}.", StandardCharsets.UTF_8);
        List<String> result = extractor.extractFromDirectory(tempDir);
        assertTrue(result.contains("name"), "Must contain 'name'");
        assertTrue(result.contains("email"), "Must contain 'email'");
    }

    @Test
    void extractsOnlyTopLevelNameForDottedExpression() throws Exception {
        Path f = tempDir.resolve("template.txt");
        Files.writeString(f, "Hello ${user.name}.", StandardCharsets.UTF_8);
        List<String> result = extractor.extractFromDirectory(tempDir);
        assertTrue(result.contains("user"), "Must contain 'user'");
        assertFalse(result.contains("user.name"), "Must NOT contain 'user.name'");
        assertFalse(result.contains("name"), "Must NOT contain 'name' from dotted expression");
    }

    @Test
    void extractsOnlyTopLevelNameForBuiltinExpression() throws Exception {
        Path f = tempDir.resolve("template.txt");
        Files.writeString(f, "${title?upper_case}", StandardCharsets.UTF_8);
        List<String> result = extractor.extractFromDirectory(tempDir);
        assertTrue(result.contains("title"), "Must contain 'title'");
        assertFalse(result.contains("upper_case"), "Must NOT contain 'upper_case'");
    }

    @Test
    void deduplicatesVariables() throws Exception {
        Path f = tempDir.resolve("template.txt");
        Files.writeString(f, "${name} ${name} ${name}", StandardCharsets.UTF_8);
        List<String> result = extractor.extractFromDirectory(tempDir);
        assertEquals(1, result.size(), "Deduplicated: should have exactly 1 entry");
        assertEquals("name", result.get(0));
    }

    @Test
    void extractsVariablesFromPathSegmentNames() throws Exception {
        Path subDir = Files.createDirectory(tempDir.resolve("${service}Controller"));
        Files.writeString(subDir.resolve("template.txt"), "Hello ${name}", StandardCharsets.UTF_8);
        List<String> result = extractor.extractFromDirectory(tempDir);
        assertTrue(result.contains("service"), "Must contain 'service' from folder name");
        assertTrue(result.contains("name"), "Must contain 'name' from file content");
    }

    @Test
    void throwsOnMalformedExpression() throws Exception {
        Path f = tempDir.resolve("broken.txt");
        Files.writeString(f, "This is ${unclosed and broken", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> extractor.extractFromDirectory(tempDir),
            "Must throw IOException for malformed expression");
    }

    @Test
    void skipsHiddenFilesAndDirectories() throws Exception {
        Path hidden = Files.createDirectory(tempDir.resolve(".git"));
        Files.writeString(hidden.resolve("config"), "${secret}", StandardCharsets.UTF_8);
        Path visible = tempDir.resolve("template.txt");
        Files.writeString(visible, "${visible}", StandardCharsets.UTF_8);
        List<String> result = extractor.extractFromDirectory(tempDir);
        assertTrue(result.contains("visible"), "Must contain 'visible' from non-hidden file");
        assertFalse(result.contains("secret"), "Must NOT contain 'secret' from hidden dir");
    }

    @Test
    void emptyDirectoryReturnsEmptyList() throws Exception {
        List<String> result = extractor.extractFromDirectory(tempDir);
        assertTrue(result.isEmpty(), "Empty directory must return empty list");
    }

    @Test
    void resultIsAlphabeticallySorted() throws Exception {
        Path f = tempDir.resolve("template.txt");
        Files.writeString(f, "${zebra} ${apple} ${mango}", StandardCharsets.UTF_8);
        List<String> result = extractor.extractFromDirectory(tempDir);
        assertEquals(List.of("apple", "mango", "zebra"), result,
            "Result must be alphabetically sorted");
    }
}
