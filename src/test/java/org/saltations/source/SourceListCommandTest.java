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
        XdgPaths xdgPaths = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
        };
        ConfigService configService = new ConfigService(xdgPaths);
        configService.init();
        FreemarkerVariableExtractor extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
        SourceListCommand command = new SourceListCommand(sourceService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void listEmptySourcesExitsZeroWithEmptyOutput() {
        int exit = commandLine.execute();
        assertEquals(0, exit, "list with no sources must exit 0");
        assertEquals("", stdout.toString().trim(), "stdout must be empty when no sources exist");
    }

    @Test
    void listWithSourcesPrintsNameAndPath() throws Exception {
        Path dir1 = Files.createDirectory(tempDir.resolve("src-alpha"));
        Path dir2 = Files.createDirectory(tempDir.resolve("src-beta"));
        sourceService.addSource("alpha", dir1.toString(), false);
        sourceService.addSource("beta", dir2.toString(), false);
        int exit = commandLine.execute();
        assertEquals(0, exit, "list with sources must exit 0");
        String output = stdout.toString();
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
        String output = stdout.toString();
        assertFalse(output.toLowerCase().contains("name"),
            "Plain list output must not have a 'Name' header");
        assertFalse(output.toLowerCase().contains("path"),
            "Plain list output must not have a 'Path' header");
    }

    @Test
    void listJsonExitsZeroAndOutputsOneLinePerSource() throws Exception {
        Path dir1 = Files.createDirectory(tempDir.resolve("j1"));
        Path dir2 = Files.createDirectory(tempDir.resolve("j2"));
        sourceService.addSource("j-alpha", dir1.toString(), false);
        sourceService.addSource("j-beta", dir2.toString(), false);
        int exit = commandLine.execute("--json");
        assertEquals(0, exit, "list --json must exit 0");
        String[] lines = stdout.toString().trim().split("\\r?\\n");
        assertEquals(2, lines.length, "JSONL output must have one line per source");
        for (String line : lines) {
            assertTrue(line.trim().startsWith("{"), "Each JSON line must start with '{'");
            assertTrue(line.trim().endsWith("}"), "Each JSON line must end with '}'");
        }
    }

    @Test
    void listJsonContainsNameAndPathKeys() throws Exception {
        Path dir = Files.createDirectory(tempDir.resolve("json-src"));
        sourceService.addSource("json-src", dir.toString(), false);
        commandLine.execute("--json");
        String output = stdout.toString();
        assertTrue(output.contains("\"name\""), "JSON output must contain 'name' key");
        assertTrue(output.contains("\"path\""), "JSON output must contain 'path' key");
        assertTrue(output.contains("\"json-src\""), "JSON output must contain source name value");
    }

    @Test
    void listExitsOneWhenConfigNotInitialized() {
        // Build a fresh commandLine pointing at uninitialized config (no configService.init())
        XdgPaths uninitPaths = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("uninit").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("uninit"); }
            @Override public Path dataDir()    { return tempDir.resolve("share2").resolve("viracocha"); }
        };
        ConfigService uninitConfig = new ConfigService(uninitPaths);
        FreemarkerVariableExtractor extractor = new FreemarkerVariableExtractor();
        SourceService uninitService = new SourceService(uninitConfig, extractor);
        SourceListCommand cmd = new SourceListCommand(uninitService);
        CommandLine cl = new CommandLine(cmd);
        ByteArrayOutputStream err2 = new ByteArrayOutputStream();
        cl.setOut(new PrintWriter(new ByteArrayOutputStream(), true));
        cl.setErr(new PrintWriter(err2, true));
        int exit = cl.execute();
        assertEquals(1, exit, "list must exit 1 when config not initialized");
        assertTrue(err2.toString().contains("Config not initialized"),
            "stderr must contain 'Config not initialized'");
    }
}
