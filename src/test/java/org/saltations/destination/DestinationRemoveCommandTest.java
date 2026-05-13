package org.saltations.destination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.XdgPaths;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for DestinationRemoveCommand.
 * No @MicronautTest — plain JUnit 5 with @TempDir XdgPaths stub.
 */
class DestinationRemoveCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private DestinationService destinationService;

    @BeforeEach
    void setUp() throws Exception {
        var xdgPaths = new XdgPaths("") {
            @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
        };
        var configService = new ConfigService(xdgPaths);
        configService.init();
        destinationService = new DestinationService(configService);
        var command = new DestinationRemoveCommand(destinationService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void removeExistingDestinationExitsZeroWithConfirmation() throws Exception {
        destinationService.addDestination("my-ws", "/some/path");
        var exit = commandLine.execute("my-ws");
        assertEquals(0, exit, "remove existing destination must exit 0");
        assertTrue(stdout.toString().contains("Destination 'my-ws' removed."),
            "stdout must contain removal confirmation message");
    }

    @Test
    void removeUnknownDestinationExitsOneWithNotFoundError() {
        var exit = commandLine.execute("ghost");
        assertEquals(1, exit, "remove unknown destination must exit 1");
        assertTrue(stderr.toString().contains("Destination 'ghost' not found."),
            "stderr must contain exact error: \"Destination 'ghost' not found.\"");
    }

    @Test
    void removeActuallyRemovesTheDestination() throws Exception {
        destinationService.addDestination("to-delete", "/some/path");
        commandLine.execute("to-delete");
        assertTrue(destinationService.listDestinations().stream()
            .noneMatch(d -> d.getName().equals("to-delete")),
            "Destination must not be in config after remove");
    }
}
