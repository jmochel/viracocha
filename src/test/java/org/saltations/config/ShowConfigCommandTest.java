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
        configService = new ConfigService(xdgPaths);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
    }

    private CommandLine buildCommandLine() {
        ShowConfigCommand command = new ShowConfigCommand(configService);
        CommandLine cl = new CommandLine(command);
        cl.setOut(new PrintWriter(stdout, true));
        cl.setErr(new PrintWriter(stderr, true));
        return cl;
    }

    @Test
    void showAfterInitPrintsConfigFileHeader() throws Exception {
        configService.init();
        Path expectedFile = configService.xdgPaths().configFile();
        int exitCode = buildCommandLine().execute();
        assertEquals(0, exitCode, "show after init must exit 0");
        String out = stdout.toString();
        assertTrue(out.startsWith("Configuration file:\n"),
            "Output must start with Configuration file header");
        assertTrue(out.contains(expectedFile.toAbsolutePath().normalize().toString()),
            "Output must contain full path to config file");
    }

    @Test
    void showAfterInitContainsYamlVersion() throws Exception {
        configService.init();
        int exitCode = buildCommandLine().execute();
        assertEquals(0, exitCode);
        assertTrue(stdout.toString().contains("version: 1"),
            "Output must contain 'version: 1' from raw YAML");
    }

    @Test
    void showBeforeInitExitsOneAndPrintsToStderr() {
        int exitCode = buildCommandLine().execute();
        assertEquals(1, exitCode, "show before init must exit 1");
        assertTrue(stderr.toString().contains("Config not initialized"),
            "Stderr must contain 'Config not initialized'");
    }
}
