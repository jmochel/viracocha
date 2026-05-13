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
        var xdgPaths = new XdgPaths(tempDir.toAbsolutePath().toString());
        var configService = new ConfigService(xdgPaths);
        configService.init();
        var extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
        destinationService = new DestinationService(configService);
        var command = new DestinationShowCommand(destinationService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void showExistingDestinationExitsZeroAndPrintsName() throws Exception {
        destinationService.addDestination("my-ws", "/some/path");
        var exit = commandLine.execute("my-ws");
        assertEquals(0, exit, "show existing destination must exit 0");
        assertTrue(stdout.toString().contains("Name:      my-ws"),
            "Output must contain 'Name:      my-ws'");
    }

    @Test
    void showOutputContainsPathLine() throws Exception {
        destinationService.addDestination("my-ws", "/some/path");
        commandLine.execute("my-ws");
        var output = stdout.toString();
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
        var output = stdout.toString();
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
    void showUnknownDestinationExitsOneWithNotFoundError() {
        var exit = commandLine.execute("x");
        assertEquals(1, exit, "show unknown destination must exit 1");
        assertTrue(stderr.toString().contains("Destination 'x' not found."),
            "stderr must contain exact error: \"Destination 'x' not found.\"");
    }
}
