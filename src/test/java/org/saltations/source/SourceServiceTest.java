package org.saltations.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.infra.XdgPaths;
import org.saltations.model.SourceEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SourceService. Uses inline XdgPaths stub and @TempDir isolation.
 * No @MicronautTest — plain JUnit 5 (D-17, per InitCommandTest pattern).
 */
class SourceServiceTest {

    @TempDir
    Path tempDir;

    private SourceService sourceService;
    private ConfigService configService;

    @BeforeEach
    void setUp() throws Exception {
        XdgPaths xdgPaths = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
        };
        configService = new ConfigService(xdgPaths);
        configService.init();  // create empty v3 config before each test
        FreemarkerVariableExtractor extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
    }

    // --- SRC-06 / D-13: path traversal rejection ---

    @Test
    void addSourceRejectsPathWithDotDot() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> sourceService.addSource("x", "/tmp/../etc", false));
        assertEquals("Path must not contain '..': /tmp/../etc", ex.getMessage());
    }

    // --- D-14: path does not exist ---

    @Test
    void addSourceRejectsNonExistentPath() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> sourceService.addSource("x", "/nonexistent/xyz/path/never", false));
        assertTrue(ex.getMessage().startsWith("Path does not exist:"),
            "Expected 'Path does not exist:' prefix, got: " + ex.getMessage());
    }

    // --- D-15: path is a file, not a directory ---

    @Test
    void addSourceRejectsFilePath() throws Exception {
        Path file = tempDir.resolve("notadir.txt");
        Files.writeString(file, "content");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> sourceService.addSource("x", file.toString(), false));
        assertEquals("Path is not a directory: " + file, ex.getMessage());
    }

    // --- SRC-05 / D-12: duplicate name rejection ---

    @Test
    void addSourceRejectsDuplicateName() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("dir1"));
        sourceService.addSource("my-source", dir.toString(), false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> sourceService.addSource("my-source", dir.toString(), false));
        assertEquals("Source 'my-source' already exists.", ex.getMessage());
    }

    // --- SRC-01: successful add returns correct SourceEntry ---

    @Test
    void addSourceSuccessReturnsEntry() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("src-dir"));
        SourceEntry result = sourceService.addSource("my-src", dir.toString(), false);
        assertEquals("my-src", result.getName());
        assertEquals(dir.toAbsolutePath().normalize().toString(), result.getPath());
        assertFalse(result.isTemplates());
        assertTrue(result.getParameters().isEmpty());
    }

    // --- SRC-07: templates=true extracts variables from directory ---

    @Test
    void addSourceWithTemplatesExtractsVariables() throws Exception {
        Path templateDir = Files.createDirectory(tempDir.resolve("templates"));
        Files.writeString(templateDir.resolve("readme.md"),
            "# ${projectName}\nAuthor: ${authorName}",
            StandardCharsets.UTF_8);
        SourceEntry result = sourceService.addSource("tmpl-src", templateDir.toString(), true);
        assertTrue(result.isTemplates());
        List<String> params = result.getParameters();
        assertTrue(params.contains("projectName"), "Expected 'projectName' in parameters");
        assertTrue(params.contains("authorName"), "Expected 'authorName' in parameters");
    }

    // --- listSources: empty and populated ---

    @Test
    void listSourcesEmptyOnFreshConfig() throws Exception {
        assertTrue(sourceService.listSources().isEmpty());
    }

    @Test
    void listSourcesReturnsAllSources() throws Exception {
        Path dir1 = Files.createDirectory(tempDir.resolve("d1"));
        Path dir2 = Files.createDirectory(tempDir.resolve("d2"));
        sourceService.addSource("alpha", dir1.toString(), false);
        sourceService.addSource("beta", dir2.toString(), false);
        List<SourceEntry> all = sourceService.listSources();
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(s -> s.getName().equals("alpha")));
        assertTrue(all.stream().anyMatch(s -> s.getName().equals("beta")));
    }

    // --- getSource ---

    @Test
    void getSourceReturnsEmptyForUnknownName() throws Exception {
        assertEquals(Optional.empty(), sourceService.getSource("nonexistent"));
    }

    @Test
    void getSourceReturnsPresentForKnownName() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("src1"));
        sourceService.addSource("known", dir.toString(), false);
        Optional<SourceEntry> result = sourceService.getSource("known");
        assertTrue(result.isPresent());
        assertEquals("known", result.get().getName());
    }

    // --- removeSource ---

    @Test
    void removeSourceReturnsFalseForUnknownName() throws Exception {
        assertFalse(sourceService.removeSource("ghost"));
    }

    @Test
    void removeSourceReturnsTrueAndRemovesEntry() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("to-remove"));
        sourceService.addSource("removeme", dir.toString(), false);
        assertTrue(sourceService.removeSource("removeme"));
        assertEquals(Optional.empty(), sourceService.getSource("removeme"));
    }
}
