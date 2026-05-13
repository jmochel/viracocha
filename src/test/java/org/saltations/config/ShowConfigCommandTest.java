package org.saltations.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.infra.XdgPaths;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for ShowConfigCommand.
 */
class ShowConfigCommandTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;

    @BeforeEach
    void setUp() {
        var xdgPaths = new XdgPaths(tempDir.toAbsolutePath().toString());
        configService = new ConfigService(xdgPaths);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
    }

    private CommandLine buildCommandLine() {
        var command = new ShowConfigCommand(configService);
        var cl = new CommandLine(command);
        cl.setOut(new PrintWriter(stdout, true));
        cl.setErr(new PrintWriter(stderr, true));
        return cl;
    }

    @Test
    void showAfterInitPrintsConfigFileHeader() throws Exception {
        configService.init();
        var expectedFile = configService.xdgPaths().configFile();
        var exitCode = buildCommandLine().execute();
        assertEquals(0, exitCode, "show after init must exit 0");
        var out = stdout.toString();
        assertTrue(out.startsWith("Configuration file:\n"),
            "Output must start with Configuration file header");
        assertTrue(out.contains(expectedFile.toAbsolutePath().normalize().toString()),
            "Output must contain full path to config file");
    }

    @Test
    void showAfterInitContainsYamlVersion() throws Exception {
        configService.init();
        var exitCode = buildCommandLine().execute();
        assertEquals(0, exitCode);
        assertTrue(stdout.toString().contains("version: 3"),
            "Output must contain 'version: 3' from raw YAML");
    }

    @Test
    void showBeforeInitExitsOneAndPrintsToStderr() {
        var exitCode = buildCommandLine().execute();
        assertEquals(1, exitCode, "show before init must exit 1");
        assertTrue(stderr.toString().contains("Config not initialized"),
            "Stderr must contain 'Config not initialized'");
    }

    @Test
    void showPrintsVersionErrorForV2Config() throws IOException {
        // Write a v1 config file to simulate a developer's existing v2 config
        var configDir = configService.xdgPaths().configDir();
        Files.createDirectories(configDir);
        var configFile = configService.xdgPaths().configFile();
        Files.writeString(configFile, "version: 1\ncatalogs: []\n");

        var exitCode = buildCommandLine().execute();
        assertEquals(1, exitCode, "show with v1 config must exit 1");
        var err = stderr.toString();
        assertTrue(err.contains("v3 format required"),
            "Stderr must contain 'v3 format required' for a v1 config file");
        assertTrue(err.contains("Error reading config:"),
            "Stderr must contain 'Error reading config:' prefix from IOException handler");
    }
}
