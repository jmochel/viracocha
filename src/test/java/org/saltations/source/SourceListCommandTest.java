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
 * Integration tests for SourceListCommand.
 * No @MicronautTest — plain JUnit 5 with @TempDir XdgPaths stub (D-17).
 */
class SourceListCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private SourceService sourceService;

    @BeforeEach
    void setUp() throws Exception {
        var xdgPaths = new XdgPaths(tempDir.toAbsolutePath().toString());
        var configService = new ConfigService(xdgPaths);
        configService.init();
        var extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
        var command = new SourceListCommand(sourceService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void listEmptySourcesExitsZeroWithEmptyOutput() {
        var exit = commandLine.execute();
        assertEquals(0, exit, "list with no sources must exit 0");
        assertEquals("", stdout.toString().trim(), "stdout must be empty when no sources exist");
    }

    @Test
    void listWithSourcesPrintsNameAndPath() throws Exception {
        Path dir1 = Files.createDirectory(tempDir.resolve("src-alpha"));
        Path dir2 = Files.createDirectory(tempDir.resolve("src-beta"));
        sourceService.addSource("alpha", dir1.toString(), false);
        sourceService.addSource("beta", dir2.toString(), false);
        var exit = commandLine.execute();
        assertEquals(0, exit, "list with sources must exit 0");
        var output = stdout.toString();
        assertTrue(output.contains("alpha"), "Output must contain source name 'alpha'");
        assertTrue(output.contains("beta"), "Output must contain source name 'beta'");
        assertTrue(output.contains(dir1.toAbsolutePath().toString()),
            "Output must contain path for alpha");
        assertTrue(output.contains(dir2.toAbsolutePath().toString()),
            "Output must contain path for beta");
    }

    @Test
    void listPlainOutputHasNoHeader() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("src1"));
        sourceService.addSource("src1", dir.toString(), false);
        commandLine.execute();
        var output = stdout.toString();
        assertFalse(output.toLowerCase().contains("name"),
            "Plain list output must not have a 'Name' header");
        assertFalse(output.toLowerCase().contains("path"),
            "Plain list output must not have a 'Path' header");
    }

    @Test
    void listExitsOneWhenConfigNotInitialized() {
        // Build a fresh commandLine pointing at uninitialized config (no configService.init())
        var uninitPaths = new XdgPaths("") {
            @Override public Path configFile() { return tempDir.resolve("uninit").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("uninit"); }
            @Override public Path dataDir()    { return tempDir.resolve("share2").resolve("viracocha"); }
        };
        
        var uninitConfig = new ConfigService(uninitPaths);
        var extractor = new FreemarkerVariableExtractor();
        var uninitService = new SourceService(uninitConfig, extractor);
        var cmd = new SourceListCommand(uninitService);
        var cl = new CommandLine(cmd);
        var err2 = new ByteArrayOutputStream();
        cl.setOut(new PrintWriter(new ByteArrayOutputStream(), true));
        cl.setErr(new PrintWriter(err2, true));
        var exit = cl.execute();
        assertEquals(1, exit, "list must exit 1 when config not initialized");
        assertTrue(err2.toString().contains("Config not initialized"),
            "stderr must contain 'Config not initialized'");
    }
}
