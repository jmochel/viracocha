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
 * Integration tests for DestinationRemoveMappingCommand.
 * Uses inline XdgPaths stub + @TempDir isolation.
 * No @MicronautTest — plain JUnit 5.
 */
class DestinationRemoveMappingCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private DestinationService destService;

    @BeforeEach
    void setUp() throws Exception {
        var xdgPaths = new XdgPaths(tempDir.toAbsolutePath().toString());
        var configService = new ConfigService(xdgPaths);
        configService.init();
        destService = new DestinationService(configService);
        var sourceService = new SourceService(configService, new FreemarkerVariableExtractor());
        // Pre-register "my-ws" destination and "my-source" source
        destService.addDestination("my-ws", "/tmp/workspace");
        Path sourceDir = Files.createDirectory(tempDir.resolve("my-source-dir"));
        sourceService.addSource("my-source", sourceDir.toString(), false);
        // Wire the command under test
        var command = new DestinationRemoveMappingCommand(destService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void removeMappingExitsZeroAndPrintsConfirmation() throws Exception {
        destService.addMapping("my-ws", "my-source", null, false, false);
        var exit = commandLine.execute("my-ws", "0");
        assertEquals(0, exit, "remove-mapping with valid index must exit 0");
        assertTrue(stdout.toString().contains("Mapping 0 removed from destination 'my-ws'."),
            "stdout must contain removal confirmation message");
    }

    @Test
    void removeMappingActuallyRemovesMapping() throws Exception {
        destService.addMapping("my-ws", "my-source", null, false, false);
        commandLine.execute("my-ws", "0");
        assertTrue(destService.listMappings("my-ws").isEmpty(),
            "Mapping must be removed from config after remove-mapping");
    }

    @Test
    void removeMappingUnknownDestinationExitsOneWithNotFoundError() {
        var exit = commandLine.execute("unknown-dest", "0");
        assertEquals(1, exit, "Unknown destination must exit 1");
        assertTrue(stderr.toString().contains("Destination 'unknown-dest' not found."),
            "stderr must contain destination not found message");
    }

    @Test
    void removeMappingOutOfRangeIndexExitsOneWithRangeError() throws Exception {
        destService.addMapping("my-ws", "my-source", null, false, false);
        var exit = commandLine.execute("my-ws", "99");
        assertEquals(1, exit, "Out-of-range index must exit 1");
        assertTrue(stderr.toString().contains("Mapping index 99 out of range"),
            "stderr must contain 'Mapping index 99 out of range'");
    }

    @Test
    void removeMappingOnEmptyDestinationExitsOneWithRangeError() {
        var exit = commandLine.execute("my-ws", "0");
        assertEquals(1, exit, "remove-mapping on empty destination must exit 1");
        assertTrue(stderr.toString().contains("Mapping index 0 out of range (destination has 0 mappings)."),
            "stderr must contain out-of-range message with zero count");
    }
}
