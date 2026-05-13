package org.saltations.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.infra.XdgPaths;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SourceShowCommand.
 * No @MicronautTest — plain JUnit 5 with @TempDir XdgPaths stub (D-17).
 */
class SourceShowCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private SourceService sourceService;

    @BeforeEach
    void setUp() throws Exception {
        var xdgPaths = new XdgPaths(tempDir.toAbsolutePath().toString());
        var configService = new ConfigService(xdgPaths);
        configService.init();
        var extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
        var command = new SourceShowCommand(sourceService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void showExistingSourceExitsZeroAndPrintsName() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("show-src"));
        sourceService.addSource("show-src", dir.toString(), false);
        var exit = commandLine.execute("show-src");
        assertEquals(0, exit, "show existing source must exit 0");
        assertTrue(stdout.toString().contains("Name:      show-src"),
            "Output must contain 'Name:      show-src'");
    }

    @Test
    void showOutputContainsPathLine() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("path-src"));
        sourceService.addSource("path-src", dir.toString(), false);
        commandLine.execute("path-src");
        var output = stdout.toString();
        assertTrue(output.contains("Path:      "),
            "Output must contain 'Path:      ' line");
        assertTrue(output.contains(dir.toAbsolutePath().toString()),
            "Output must contain the source directory path");
    }

    @Test
    void showOutputContainsTemplatesFalseForNonTemplateSource() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("non-tmpl"));
        sourceService.addSource("non-tmpl", dir.toString(), false);
        commandLine.execute("non-tmpl");
        assertTrue(stdout.toString().contains("Templates: false"),
            "Output must contain 'Templates: false' for non-template source");
    }

    @Test
    void showNonTemplateSourceOmitsParametersBlock() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("no-params"));
        sourceService.addSource("no-params", dir.toString(), false);
        commandLine.execute("no-params");
        assertFalse(stdout.toString().contains("Parameters:"),
            "Non-template source output must NOT contain 'Parameters:' block");
    }

    @Test
    void showTemplateSourceWithVarsIncludesParametersBlock() throws Exception {
        Path templateDir = Files.createDirectory(tempDir.resolve("tmpl-with-vars"));
        Files.writeString(templateDir.resolve("tpl.md"),
            "Hello ${firstName}, your project is ${projectName}",
            StandardCharsets.UTF_8);
        sourceService.addSource("tmpl-vars", templateDir.toString(), true);
        commandLine.execute("tmpl-vars");
        var output = stdout.toString();
        assertTrue(output.contains("Templates: true"),
            "Output must contain 'Templates: true'");
        assertTrue(output.contains("Parameters:"),
            "Template source with vars must include 'Parameters:' block");
        assertTrue(output.contains("  firstName") || output.contains("  projectName"),
            "Parameters block must contain indented variable names");
    }

    @Test
    void showTemplateSourceWithEmptyParamsOmitsParametersBlock() throws Exception {
        // Template source directory exists but contains no template variables
        Path templateDir = Files.createDirectory(tempDir.resolve("tmpl-empty"));
        Files.writeString(templateDir.resolve("plain.txt"), "no variables here",
            StandardCharsets.UTF_8);
        sourceService.addSource("tmpl-empty", templateDir.toString(), true);
        commandLine.execute("tmpl-empty");
        assertFalse(stdout.toString().contains("Parameters:"),
            "Template source with empty parameters must NOT include 'Parameters:' block (D-06)");
    }

    @Test
    void showMissingSourceExitsOneWithNotFoundError() {
        var exit = commandLine.execute("no-such-source");
        assertEquals(1, exit, "show missing source must exit 1");
        assertTrue(stderr.toString().contains("Source 'no-such-source' not found."),
            "stderr must contain exact D-16 error: \"Source 'no-such-source' not found.\"");
    }
}
