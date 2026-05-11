package org.saltations.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.infra.XdgPaths;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SourceRemoveCommand.
 * No @MicronautTest — plain JUnit 5 with @TempDir XdgPaths stub (D-17).
 */
class SourceRemoveCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
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
        FreemarkerVariableExtractor extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
        SourceRemoveCommand command = new SourceRemoveCommand(sourceService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void removeExistingSourceExitsZeroWithConfirmation() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("to-remove"));
        sourceService.addSource("to-remove", dir.toString(), false);
        int exit = commandLine.execute("to-remove");
        assertEquals(0, exit, "remove existing source must exit 0");
        assertTrue(stdout.toString().contains("to-remove"),
            "Confirmation message must mention the source name");
    }

    @Test
    void removeExistingSourceDeletesItFromConfig() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("deleted-src"));
        sourceService.addSource("deleted-src", dir.toString(), false);
        commandLine.execute("deleted-src");
        // Source must no longer appear in list
        assertTrue(sourceService.listSources().stream()
            .noneMatch(s -> s.getName().equals("deleted-src")),
            "Source must not be in config after remove");
    }

    @Test
    void removeMissingSourceExitsOneWithNotFoundError() {
        int exit = commandLine.execute("no-such-source");
        assertEquals(1, exit, "remove missing source must exit 1");
        assertTrue(stderr.toString().contains("Source 'no-such-source' not found."),
            "stderr must contain exact D-16 error: \"Source 'no-such-source' not found.\"");
    }

    @Test
    void removeExitsOneWhenConfigNotInitialized() {
        XdgPaths uninitPaths = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("uninit2").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("uninit2"); }
            @Override public Path dataDir()    { return tempDir.resolve("share2").resolve("viracocha"); }
        };
        ConfigService uninitConfig = new ConfigService(uninitPaths);
        FreemarkerVariableExtractor extractor = new FreemarkerVariableExtractor();
        SourceService uninitService = new SourceService(uninitConfig, extractor);
        SourceRemoveCommand cmd = new SourceRemoveCommand(uninitService);
        CommandLine cl = new CommandLine(cmd);
        ByteArrayOutputStream err2 = new ByteArrayOutputStream();
        cl.setOut(new PrintWriter(new ByteArrayOutputStream(), true));
        cl.setErr(new PrintWriter(err2, true));
        int exit = cl.execute("anything");
        assertEquals(1, exit, "remove must exit 1 when config not initialized");
        assertTrue(err2.toString().contains("Config not initialized"),
            "stderr must contain 'Config not initialized'");
    }

    @Test
    void missingNameArgumentExitsNonZero() {
        int exit = commandLine.execute();  // no positional arg provided
        assertNotEquals(0, exit, "Missing NAME argument must exit non-zero");
    }
}
