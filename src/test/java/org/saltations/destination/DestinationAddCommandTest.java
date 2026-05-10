package org.saltations.destination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.XdgPaths;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DestinationAddCommand.
 * Uses inline XdgPaths stub + @TempDir isolation.
 * No @MicronautTest — plain JUnit 5.
 */
class DestinationAddCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private ConfigService configService;

    @BeforeEach
    void setUp() throws Exception {
        XdgPaths xdgPaths = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
        };
        configService = new ConfigService(xdgPaths);
        configService.init();
        DestinationService destinationService = new DestinationService(configService);
        DestinationAddCommand command = new DestinationAddCommand(destinationService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void addValidDestinationExitsZeroAndPrintsConfirmation() {
        int exit = commandLine.execute("--name", "my-ws", "--path", "/tmp/workspace");
        assertEquals(0, exit, "Valid destination add must exit 0");
        assertTrue(stdout.toString().contains("Destination 'my-ws' added."),
            "stdout must contain confirmation message");
    }

    @Test
    void addValidDestinationPersistsToConfig() throws IOException {
        commandLine.execute("--name", "persisted-dest", "--path", "/tmp/workspaces/my-ws");
        String yaml = Files.readString(configService.xdgPaths().configFile());
        assertTrue(yaml.contains("persisted-dest"), "Config YAML must contain destination name after add");
    }

    @Test
    void addPathWithDotDotExitsOneWithTraversalError() {
        int exit = commandLine.execute("--name", "x", "--path", "/tmp/../etc");
        assertEquals(1, exit, "Path with '..' must exit 1");
        assertTrue(stderr.toString().contains("Path must not contain '..'"),
            "stderr must contain traversal error message");
    }

    @Test
    void addDuplicateNameExitsOneWithAlreadyExistsError() {
        commandLine.execute("--name", "dup-dest", "--path", "/tmp/ws1");
        stdout.reset();
        int exit = commandLine.execute("--name", "dup-dest", "--path", "/tmp/ws2");
        assertEquals(1, exit, "Duplicate name must exit 1");
        assertTrue(stderr.toString().contains("already exists"),
            "stderr must contain 'already exists' message");
    }

    @Test
    void addTildePathExitsZeroPathStoredAsIs() {
        int exit = commandLine.execute("--name", "tilde-dest", "--path", "~/workspace");
        assertEquals(0, exit, "Tilde path must exit 0 (no existence check for destinations)");
        assertTrue(stdout.toString().contains("Destination 'tilde-dest' added."),
            "stdout must contain confirmation message for tilde path");
    }

    @Test
    void missingNameOptionExitsNonZero() {
        int exit = commandLine.execute("--path", "/tmp/workspace");
        assertNotEquals(0, exit, "Missing --name must exit non-zero");
    }

    @Test
    void missingPathOptionExitsNonZero() {
        int exit = commandLine.execute("--name", "x");
        assertNotEquals(0, exit, "Missing --path must exit non-zero");
    }
}
