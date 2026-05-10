package org.saltations.destination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.infra.XdgPaths;
import org.saltations.source.SourceService;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DestinationShowCommand.
 * No @MicronautTest — plain JUnit 5 with @TempDir XdgPaths stub.
 * For mapping tests: source must be registered first (addMapping validates sourceRef).
 */
class DestinationShowCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private DestinationService destinationService;
    private SourceService sourceService;

    @BeforeEach
    void setUp() throws Exception {
        XdgPaths xdgPaths = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
        };
        ConfigService configService = new ConfigService(xdgPaths);
        configService.init();
        FreemarkerVariableExtractor extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
        destinationService = new DestinationService(configService);
        DestinationShowCommand command = new DestinationShowCommand(destinationService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void showExistingDestinationExitsZeroAndPrintsName() throws Exception {
        destinationService.addDestination("my-ws", "/some/path");
        int exit = commandLine.execute("my-ws");
        assertEquals(0, exit, "show existing destination must exit 0");
        assertTrue(stdout.toString().contains("Name:      my-ws"),
            "Output must contain 'Name:      my-ws'");
    }

    @Test
    void showOutputContainsPathLine() throws Exception {
        destinationService.addDestination("my-ws", "/some/path");
        commandLine.execute("my-ws");
        String output = stdout.toString();
        assertTrue(output.contains("Path:      /some/path"),
            "Output must contain 'Path:      /some/path'");
    }

    @Test
    void showDestinationWithEmptyParametersOmitsParametersBlock() throws Exception {
        destinationService.addDestination("no-params-ws", "/some/path");
        commandLine.execute("no-params-ws");
        assertFalse(stdout.toString().contains("Parameters:"),
            "Destination with empty parameters must NOT contain 'Parameters:' block (D-11)");
    }

    @Test
    void showDestinationWithNoMappingsContainsMappingsNone() throws Exception {
        destinationService.addDestination("empty-ws", "/some/path");
        commandLine.execute("empty-ws");
        assertTrue(stdout.toString().contains("Mappings: (none)"),
            "Destination with no mappings must contain 'Mappings: (none)'");
    }

    @Test
    void showDestinationWithOneMappingContainsMappingFields() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("some-source-dir"));
        sourceService.addSource("some-source", sourceDir.toString(), false);
        destinationService.addDestination("mapped-ws", "/some/path");
        destinationService.addMapping("mapped-ws", "some-source", null, false, false);
        commandLine.execute("mapped-ws");
        String output = stdout.toString();
        assertTrue(output.contains("Mapping 1:"), "Output must contain 'Mapping 1:'");
        assertTrue(output.contains("Source:  some-source"), "Output must contain 'Source:  some-source'");
        assertTrue(output.contains("Glob:    (all files)"), "Null glob must display as '(all files)' per D-13");
        assertTrue(output.contains("Recurse: false"), "Output must contain 'Recurse: false'");
        assertTrue(output.contains("Sync:    false"), "Output must contain 'Sync:    false'");
    }

    @Test
    void showDestinationWithGlobPatternDisplaysGlob() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("glob-source-dir"));
        sourceService.addSource("glob-source", sourceDir.toString(), false);
        destinationService.addDestination("glob-ws", "/some/path");
        destinationService.addMapping("glob-ws", "glob-source", "**/*.md", false, false);
        commandLine.execute("glob-ws");
        assertTrue(stdout.toString().contains("Glob:    **/*.md"),
            "Output must contain 'Glob:    **/*.md'");
    }

    @Test
    void showJsonOutputExitsZeroAndIsValidJsonObject() throws Exception {
        destinationService.addDestination("json-ws", "/some/path");
        int exit = commandLine.execute("--json", "json-ws");
        assertEquals(0, exit, "show --json must exit 0");
        String output = stdout.toString().trim();
        assertTrue(output.startsWith("{"), "JSON output must start with '{'");
        assertTrue(output.endsWith("}"), "JSON output must end with '}'");
        assertTrue(output.contains("\"name\""), "JSON must contain 'name' key");
    }

    @Test
    void showUnknownDestinationExitsOneWithNotFoundError() {
        int exit = commandLine.execute("x");
        assertEquals(1, exit, "show unknown destination must exit 1");
        assertTrue(stderr.toString().contains("Destination 'x' not found."),
            "stderr must contain exact error: \"Destination 'x' not found.\"");
    }
}
