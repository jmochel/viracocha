package org.saltations.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.infra.XdgPaths;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for InitCommand.
 * Uses @TempDir to isolate from real XDG config. Tests CommandLine.execute() return value.
 */
class InitCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;

    @BeforeEach
    void setUp() {
        XdgPaths xdgPaths = new XdgPaths() {
            @Override
            public Path configFile() {
                return tempDir.resolve("viracocha").resolve("config.yaml");
            }
            @Override
            public Path configDir() {
                return tempDir.resolve("viracocha");
            }
            @Override
            public Path dataDir() {
                return tempDir.resolve("share").resolve("viracocha");
            }
        };
        ConfigService configService = new ConfigService(xdgPaths);
        InitCommand command = new InitCommand(configService);
        commandLine = new CommandLine(command);

        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void initOnFreshDirPrintsConfirmationAndExitsZero() {
        int exitCode = commandLine.execute();
        assertEquals(0, exitCode, "init on fresh dir must exit 0");
        assertTrue(stdout.toString().contains("Config initialized at"),
            "Output must contain 'Config initialized at'");
    }

    @Test
    void reInitPrintsAlreadyInitializedAndExitsZero() {
        commandLine.execute(); // first init
        stdout.reset();
        int exitCode = commandLine.execute(); // second init
        assertEquals(0, exitCode, "re-init must exit 0");
        assertTrue(stdout.toString().contains("Config already initialized at"),
            "Re-init output must contain 'Config already initialized at'");
    }
}
