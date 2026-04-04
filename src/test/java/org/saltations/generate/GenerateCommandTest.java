package org.saltations.generate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.XdgPaths;
import org.saltations.model.MappingEntry;
import org.saltations.model.PatternEntry;
import org.saltations.model.ProjectEntry;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GenerateCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private ConfigService configService;

    private XdgPaths xdgPaths() {
        return new XdgPaths() {
            @Override
            public Path configFile() {
                return tempDir.resolve("viracocha/config.yaml");
            }

            @Override
            public Path configDir() {
                return tempDir.resolve("viracocha");
            }

            @Override
            public Path dataDir() {
                return tempDir.resolve("share/viracocha");
            }
        };
    }

    @BeforeEach
    void setUp() throws Exception {
        configService = new ConfigService(xdgPaths());
        configService.init();
        GeneratorService generatorService = new GeneratorService(configService, new PathExpander());
        GenerateCommand cmd = new GenerateCommand(generatorService);
        commandLine = new CommandLine(cmd);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    private void seedProject() throws Exception {
        Path workspace = Files.createDirectory(tempDir.resolve("ws"));
        Path patternDir = Files.createDirectory(tempDir.resolve("pat"));
        Files.writeString(patternDir.resolve("f.txt"), "x", StandardCharsets.UTF_8);

        ViracochaConfig cfg = new ViracochaConfig();
        cfg.getPatterns().add(new PatternEntry("pat1", patternDir.toString(), List.of()));
        cfg.getProjects().add(new ProjectEntry("p1", workspace.toString(),
            List.of(new MappingEntry("pat1", ".", new LinkedHashMap<>())), new LinkedHashMap<>()));
        configService.save(cfg);
    }

    @Test
    void printsSummaryLineWithCounts() throws Exception {
        seedProject();
        int exit = commandLine.execute("--project-name", "p1");
        assertEquals(0, exit);
        String out = stdout.toString();
        assertTrue(out.contains("Generated:"));
        assertTrue(out.contains("Skipped:"));
        assertTrue(out.contains("Failed:"));
    }

    @Test
    void verbosePrintsActionLinesBeforeSummary() throws Exception {
        seedProject();
        int exit = commandLine.execute("--project-name", "p1", "--verbose");
        assertEquals(0, exit);
        String out = stdout.toString();
        assertTrue(out.contains("Created ") || out.contains("Skipped "));
        assertTrue(out.contains("Generated:"));
    }

    @Test
    void dryRunDoesNotAddFiles() throws Exception {
        seedProject();
        Path workspace = tempDir.resolve("ws");
        long before = fileCount(workspace);
        int exit = commandLine.execute("--project-name", "p1", "--dry-run");
        assertEquals(0, exit);
        assertEquals(before, fileCount(workspace));
        assertTrue(stdout.toString().contains("Generated:"));
    }

    @Test
    void dryRunWithVerboseStillNoWrites() throws Exception {
        seedProject();
        Path workspace = tempDir.resolve("ws");
        long before = fileCount(workspace);
        int exit = commandLine.execute("--project-name", "p1", "--dry-run", "--verbose");
        assertEquals(0, exit);
        assertEquals(before, fileCount(workspace));
    }

    @Test
    void configNotInitializedExitsOne() {
        XdgPaths uninit = new XdgPaths() {
            @Override
            public Path configFile() {
                return tempDir.resolve("noinit/config.yaml");
            }

            @Override
            public Path configDir() {
                return tempDir.resolve("noinit");
            }

            @Override
            public Path dataDir() {
                return tempDir.resolve("noinit/share");
            }
        };
        GenerateCommand cmd = new GenerateCommand(new GeneratorService(new ConfigService(uninit), new PathExpander()));
        CommandLine cl = new CommandLine(cmd);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        cl.setErr(new PrintWriter(err, true));
        int exit = cl.execute("--project-name", "x");
        assertEquals(1, exit);
        assertTrue(err.toString().contains("Config not initialized"));
    }

    private static long fileCount(Path root) throws Exception {
        if (!Files.exists(root)) {
            return 0;
        }
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(Files::isRegularFile).count();
        }
    }
}
