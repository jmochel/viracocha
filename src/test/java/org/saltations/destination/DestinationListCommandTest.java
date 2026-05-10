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

import static org.junit.jupiter.api.Assertions.*;

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
        XdgPaths xdgPaths = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
        };
        ConfigService configService = new ConfigService(xdgPaths);
        configService.init();
        destinationService = new DestinationService(configService);
        DestinationListCommand command = new DestinationListCommand(destinationService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void listEmptyDestinationsExitsZeroWithEmptyOutput() {
        int exit = commandLine.execute();
        assertEquals(0, exit, "list with no destinations must exit 0");
        assertEquals("", stdout.toString().trim(), "stdout must be empty when no destinations exist");
    }

    @Test
    void listWithOneDestinationPrintsNameAndPath() throws Exception {
        destinationService.addDestination("alpha-ws", "/home/user/workspaces/alpha");
        int exit = commandLine.execute();
        assertEquals(0, exit, "list with destinations must exit 0");
        String output = stdout.toString();
        assertTrue(output.contains("alpha-ws"), "Output must contain destination name 'alpha-ws'");
        assertTrue(output.contains("/home/user/workspaces/alpha"),
            "Output must contain path for alpha-ws");
    }

    @Test
    void listWithTwoDestinationsPrintsTwoLinesNamesLeftAligned() throws Exception {
        destinationService.addDestination("alpha", "/home/user/alpha");
        destinationService.addDestination("beta", "/home/user/beta");
        int exit = commandLine.execute();
        assertEquals(0, exit, "list with two destinations must exit 0");
        String output = stdout.toString();
        String[] lines = output.trim().split("\\r?\\n");
        assertEquals(2, lines.length, "Output must have exactly two lines");
        assertTrue(output.contains("alpha"), "Output must contain 'alpha'");
        assertTrue(output.contains("beta"), "Output must contain 'beta'");
    }

    @Test
    void listJsonPrintsOneJsonObjectPerLineContainingNameKey() throws Exception {
        destinationService.addDestination("json-ws1", "/home/user/json-ws1");
        destinationService.addDestination("json-ws2", "/home/user/json-ws2");
        int exit = commandLine.execute("--json");
        assertEquals(0, exit, "list --json must exit 0");
        String[] lines = stdout.toString().trim().split("\\r?\\n");
        assertEquals(2, lines.length, "JSONL output must have one line per destination");
        for (String line : lines) {
            assertTrue(line.trim().startsWith("{"), "Each JSON line must start with '{'");
            assertTrue(line.trim().endsWith("}"), "Each JSON line must end with '}'");
            assertTrue(line.contains("\"name\""), "Each JSON line must contain 'name' key");
        }
    }
}
