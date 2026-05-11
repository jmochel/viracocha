package org.saltations.destination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.infra.XdgPaths;
import org.saltations.model.MappingEntry;
import org.saltations.source.SourceService;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DestinationAddMappingCommand.
 * Uses inline XdgPaths stub + @TempDir isolation.
 * No @MicronautTest — plain JUnit 5.
 */
class DestinationAddMappingCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private DestinationService destService;
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
        destService = new DestinationService(configService);
        sourceService = new SourceService(configService, new FreemarkerVariableExtractor());
        // Pre-register "my-ws" destination and "my-source" source
        destService.addDestination("my-ws", "/tmp/workspace");
        Path sourceDir = Files.createDirectory(tempDir.resolve("my-source-dir"));
        sourceService.addSource("my-source", sourceDir.toString(), false);
        // Wire the command under test
        DestinationAddMappingCommand command = new DestinationAddMappingCommand(destService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void addMappingExitsZeroAndPrintsConfirmation() {
        int exit = commandLine.execute("my-ws", "--source", "my-source");
        assertEquals(0, exit, "Valid add-mapping must exit 0");
        assertTrue(stdout.toString().contains("Mapping added to destination 'my-ws'."),
            "stdout must contain confirmation message");
    }

    @Test
    void addMappingPersistsToConfig() throws Exception {
        commandLine.execute("my-ws", "--source", "my-source");
        List<MappingEntry> mappings = destService.listMappings("my-ws");
        assertFalse(mappings.isEmpty(), "Mapping must be persisted to config");
        assertEquals("my-source", mappings.get(0).getSourceRef(), "sourceRef must match");
    }

    @Test
    void addMappingWithAllFlagsPersistsAllFields() throws Exception {
        int exit = commandLine.execute("my-ws", "--source", "my-source",
            "--glob", "**/*.md", "--recurse", "--sync");
        assertEquals(0, exit, "add-mapping with all flags must exit 0");
        List<MappingEntry> mappings = destService.listMappings("my-ws");
        assertFalse(mappings.isEmpty(), "Mapping must be persisted");
        MappingEntry m = mappings.get(0);
        assertEquals("my-source", m.getSourceRef(), "sourceRef must match");
        assertEquals("**/*.md", m.getGlob(), "glob must match");
        assertTrue(m.isRecurse(), "--recurse must be persisted");
        assertTrue(m.isSync(), "--sync must be persisted");
    }

    @Test
    void addMappingUnknownDestinationExitsOneWithNotFoundError() {
        int exit = commandLine.execute("unknown-dest", "--source", "my-source");
        assertEquals(1, exit, "Unknown destination must exit 1");
        assertTrue(stderr.toString().contains("Destination 'unknown-dest' not found."),
            "stderr must contain destination not found message");
    }

    @Test
    void addMappingUnknownSourceExitsOneWithNotFoundError() {
        int exit = commandLine.execute("my-ws", "--source", "unknown-source");
        assertEquals(1, exit, "Unknown source must exit 1");
        assertTrue(stderr.toString().contains("Source 'unknown-source' not found."),
            "stderr must contain source not found message");
    }

    @Test
    void missingSourceOptionExitsNonZero() {
        int exit = commandLine.execute("my-ws");
        assertNotEquals(0, exit, "Missing --source must exit non-zero");
    }
}
