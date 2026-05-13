package org.saltations.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.infra.XdgPaths;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SourceAddCommand.
 * Uses inline XdgPaths stub + @TempDir isolation (D-17, per InitCommandTest pattern).
 * No @MicronautTest — plain JUnit 5.
 */
class SourceAddCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private ConfigService configService;

    @BeforeEach
    void setUp() throws Exception {
        var xdgPaths = new XdgPaths(tempDir.toAbsolutePath().toString());
        configService = new ConfigService(xdgPaths);
        configService.init();
        var extractor = new FreemarkerVariableExtractor();
        var sourceService = new SourceService(configService, extractor);
        var command = new SourceAddCommand(sourceService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void addValidSourceExitsZeroAndPrintsConfirmation() throws IOException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("my-source"));
        var exit = commandLine.execute("--name", "my-source", "--path", sourceDir.toString());
        assertEquals(0, exit, "Valid source add must exit 0");
        assertTrue(stdout.toString().contains("Source 'my-source' added."),
            "stdout must contain confirmation message");
    }

    @Test
    void addValidSourcePersistsToConfig() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("persisted-src"));
        commandLine.execute("--name", "persisted-src", "--path", sourceDir.toString());
        // Read config YAML and verify source persisted
        String yaml = Files.readString(configService.xdgPaths().configFile());
        assertTrue(yaml.contains("persisted-src"), "Config YAML must contain source name after add");
    }

    @Test
    void addWithTemplatesFlagExtractsVariablesAndPersists() throws Exception {
        Path templateDir = Files.createDirectory(tempDir.resolve("tmpl-dir"));
        Files.writeString(templateDir.resolve("template.md"),
            "# ${projectName}\nby ${authorName}", StandardCharsets.UTF_8);
        var exit = commandLine.execute("--name", "tmpl", "--path", templateDir.toString(), "--has-templates");
        assertEquals(0, exit, "source add --has-templates must exit 0");
        // Verify parameters appear in config
        String yaml = Files.readString(configService.xdgPaths().configFile());
        assertTrue(yaml.contains("projectName") || yaml.contains("authorName"),
            "Config YAML must contain extracted variable names");
    }

    @Test
    void addWithShortAliasesExitsZero() throws IOException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("alias-source"));
        var exit = commandLine.execute("-n", "alias-source", "-p", sourceDir.toString());
        assertEquals(0, exit, "Short aliases -n/-p must exit 0");
        assertTrue(stdout.toString().contains("Source 'alias-source' added."),
            "stdout must contain confirmation message when using short aliases");
    }

    @Test
    void addPathWithDotDotExitsOneWithTraversalError() {
        var exit = commandLine.execute("--name", "x", "--path", "/tmp/../etc");
        assertEquals(1, exit, "Path with '..' must exit 1");
        assertTrue(stderr.toString().contains("Path must not contain '..'"),
            "stderr must contain traversal error message");
    }

    @Test
    void addDuplicateNameExitsOneWithAlreadyExistsError() throws IOException {
        Path dir1 = Files.createDirectory(tempDir.resolve("dir1"));
        commandLine.execute("--name", "dup-source", "--path", dir1.toString());
        stdout.reset();
        var exit = commandLine.execute("--name", "dup-source", "--path", dir1.toString());
        assertEquals(1, exit, "Duplicate name must exit 1");
        assertTrue(stderr.toString().contains("already exists"),
            "stderr must contain 'already exists' message");
    }

    @Test
    void addNonExistentPathExitsOneWithDoesNotExistError() {
        var exit = commandLine.execute("--name", "x", "--path", "/nonexistent/xyz/never");
        assertEquals(1, exit, "Non-existent path must exit 1");
        assertTrue(stderr.toString().contains("Path does not exist:"),
            "stderr must contain 'Path does not exist:' message");
    }

    @Test
    void missingNameOptionExitsNonZero() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("some-dir"));
        var exit = commandLine.execute("--path", dir.toString());
        assertNotEquals(0, exit, "Missing --name must exit non-zero");
    }

    @Test
    void missingPathOptionExitsNonZero() {
        var exit = commandLine.execute("--name", "x");
        assertNotEquals(0, exit, "Missing --path must exit non-zero");
    }
}
