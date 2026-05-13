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
 * Integration tests for DestinationListCommand.
 * No @MicronautTest — plain JUnit 5 with @TempDir XdgPaths stub.
 */
class DestinationListCommandTest {

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
        var command = new DestinationListCommand(destinationService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void listEmptyDestinationsExitsZeroWithEmptyOutput() {
        var exit = commandLine.execute();
        assertEquals(0, exit, "list with no destinations must exit 0");
        assertEquals("", stdout.toString().trim(), "stdout must be empty when no destinations exist");
    }

    @Test
    void listWithOneDestinationPrintsNameAndPath() throws Exception {
        destinationService.addDestination("alpha-ws", "/home/user/workspaces/alpha");
        var exit = commandLine.execute();
        assertEquals(0, exit, "list with destinations must exit 0");
        var output = stdout.toString();
        assertTrue(output.contains("alpha-ws"), "Output must contain destination name 'alpha-ws'");
        assertTrue(output.contains("/home/user/workspaces/alpha"),
            "Output must contain path for alpha-ws");
    }

    @Test
    void listWithTwoDestinationsPrintsTwoLinesNamesLeftAligned() throws Exception {
        destinationService.addDestination("alpha", "/home/user/alpha");
        destinationService.addDestination("beta", "/home/user/beta");
        var exit = commandLine.execute();
        assertEquals(0, exit, "list with two destinations must exit 0");
        var output = stdout.toString();
        var lines = output.trim().split("\\r?\\n");
        assertEquals(2, lines.length, "Output must have exactly two lines");
        assertTrue(output.contains("alpha"), "Output must contain 'alpha'");
        assertTrue(output.contains("beta"), "Output must contain 'beta'");
    }

}
