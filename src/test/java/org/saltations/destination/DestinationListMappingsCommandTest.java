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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for DestinationListMappingsCommand.
 * Uses inline XdgPaths stub + @TempDir isolation.
 * No @MicronautTest — plain JUnit 5.
 */
class DestinationListMappingsCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private DestinationService destService;

    @BeforeEach
    void setUp() throws Exception {
        var xdgPaths = new XdgPaths("") {
            @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
        };
        var configService = new ConfigService(xdgPaths);
        configService.init();
        destService = new DestinationService(configService);
        var sourceService = new SourceService(configService, new FreemarkerVariableExtractor());
        // Pre-register "my-ws" destination and "my-source" source
        destService.addDestination("my-ws", "/tmp/workspace");
        Path sourceDir = Files.createDirectory(tempDir.resolve("my-source-dir"));
        sourceService.addSource("my-source", sourceDir.toString(), false);
        // Wire the command under test
        var command = new DestinationListMappingsCommand(destService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void listMappingsWithOneMappingPrintsMappingBlock() throws Exception {
        destService.addMapping("my-ws", "my-source", null, false, false);
        var exit = commandLine.execute("my-ws");
        assertEquals(0, exit, "list-mappings must exit 0");
        var out = stdout.toString();
        assertTrue(out.contains("Mapping 1:"), "stdout must contain 'Mapping 1:'");
        assertTrue(out.contains("Source:  my-source"), "stdout must contain source ref");
    }

    @Test
    void listMappingsWithNullGlobPrintsAllFilesPlaceholder() throws Exception {
        destService.addMapping("my-ws", "my-source", null, false, false);
        commandLine.execute("my-ws");
        var out = stdout.toString();
        assertTrue(out.contains("Glob:    (all files)"), "null glob must display as '(all files)'");
    }

    @Test
    void listMappingsWithNoMappingsPrintsEmptyMessage() throws Exception {
        var exit = commandLine.execute("my-ws");
        assertEquals(0, exit, "list-mappings with no mappings must exit 0");
        assertTrue(stdout.toString().contains("No mappings for destination 'my-ws'."),
            "stdout must contain no-mappings message");
    }

    @Test
    void listMappingsShowsRecurseAndSyncFields() throws Exception {
        destService.addMapping("my-ws", "my-source", null, false, false);
        commandLine.execute("my-ws");
        var out = stdout.toString();
        assertTrue(out.contains("Recurse: false"), "stdout must contain 'Recurse: false'");
        assertTrue(out.contains("Sync:    false"), "stdout must contain 'Sync:    false'");
    }
}
