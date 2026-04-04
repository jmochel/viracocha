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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorServiceTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private GeneratorService generatorService;

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
        generatorService = new GeneratorService(configService, new PathExpander());
    }

    @Test
    void generatesExpandedFileFromPattern() throws Exception {
        Path workspace = Files.createDirectory(tempDir.resolve("ws"));
        Path patternDir = Files.createDirectory(tempDir.resolve("pat"));
        Files.createDirectories(patternDir.resolve("sub"));
        Files.writeString(patternDir.resolve("sub/${name}.txt"), "Hello ${name}!", StandardCharsets.UTF_8);

        ViracochaConfig cfg = new ViracochaConfig();
        cfg.getPatterns().add(new PatternEntry("pat1", patternDir.toString(), List.of()));
        LinkedHashMap<String, String> mParams = new LinkedHashMap<>();
        mParams.put("name", "World");
        cfg.getProjects().add(new ProjectEntry("p1", workspace.toString(),
            List.of(new MappingEntry("pat1", ".", mParams)), new LinkedHashMap<>()));
        configService.save(cfg);

        GenerationResult r = generatorService.generate("p1", false, false);
        assertEquals(1, r.generated());
        assertEquals(0, r.skipped());
        assertEquals(0, r.failed());

        Path written = workspace.resolve("sub/World.txt");
        assertTrue(Files.isRegularFile(written));
        assertEquals("Hello World!", Files.readString(written, StandardCharsets.UTF_8));
    }

    @Test
    void secondRunSkipsExistingFile() throws Exception {
        Path workspace = Files.createDirectory(tempDir.resolve("ws"));
        Path patternDir = Files.createDirectory(tempDir.resolve("pat"));
        Files.writeString(patternDir.resolve("a.txt"), "x", StandardCharsets.UTF_8);

        ViracochaConfig cfg = new ViracochaConfig();
        cfg.getPatterns().add(new PatternEntry("pat1", patternDir.toString(), List.of()));
        cfg.getProjects().add(new ProjectEntry("p1", workspace.toString(),
            List.of(new MappingEntry("pat1", ".", new LinkedHashMap<>())), new LinkedHashMap<>()));
        configService.save(cfg);

        assertEquals(0, generatorService.generate("p1", false, false).skipped());
        GenerationResult second = generatorService.generate("p1", false, false);
        assertTrue(second.skipped() >= 1);
        assertEquals(0, second.generated());
    }

    @Test
    void dryRunLeavesWorkspaceUnchanged() throws Exception {
        Path workspace = Files.createDirectory(tempDir.resolve("ws"));
        Path patternDir = Files.createDirectory(tempDir.resolve("pat"));
        Files.writeString(patternDir.resolve("only.txt"), "body", StandardCharsets.UTF_8);

        ViracochaConfig cfg = new ViracochaConfig();
        cfg.getPatterns().add(new PatternEntry("pat1", patternDir.toString(), List.of()));
        cfg.getProjects().add(new ProjectEntry("p1", workspace.toString(),
            List.of(new MappingEntry("pat1", ".", new LinkedHashMap<>())), new LinkedHashMap<>()));
        configService.save(cfg);

        long before = fileCount(workspace);
        GenerationResult r = generatorService.generate("p1", true, false);
        assertEquals(1, r.generated());
        long after = fileCount(workspace);
        assertEquals(before, after);
    }

    @Test
    void dryRunStillReportsWouldCreateInVerbose() throws Exception {
        Path workspace = Files.createDirectory(tempDir.resolve("ws"));
        Path patternDir = Files.createDirectory(tempDir.resolve("pat"));
        Files.writeString(patternDir.resolve("only.txt"), "b", StandardCharsets.UTF_8);

        ViracochaConfig cfg = new ViracochaConfig();
        cfg.getPatterns().add(new PatternEntry("pat1", patternDir.toString(), List.of()));
        cfg.getProjects().add(new ProjectEntry("p1", workspace.toString(),
            List.of(new MappingEntry("pat1", ".", new LinkedHashMap<>())), new LinkedHashMap<>()));
        configService.save(cfg);

        GenerationResult r = generatorService.generate("p1", true, true);
        assertEquals(1, r.generated());
        assertTrue(r.verboseLines().stream().anyMatch(l -> l.startsWith("Created ")));
    }

    @Test
    void fileBlocksDirectoryCountsFailed() throws Exception {
        Path workspace = Files.createDirectory(tempDir.resolve("ws"));
        Files.writeString(workspace.resolve("out"), "", StandardCharsets.UTF_8);

        Path patternDir = Files.createDirectory(tempDir.resolve("pat"));
        Files.writeString(patternDir.resolve("x.txt"), "z", StandardCharsets.UTF_8);

        ViracochaConfig cfg = new ViracochaConfig();
        cfg.getPatterns().add(new PatternEntry("pat1", patternDir.toString(), List.of()));
        cfg.getProjects().add(new ProjectEntry("p1", workspace.toString(),
            List.of(new MappingEntry("pat1", "out", new LinkedHashMap<>())), new LinkedHashMap<>()));
        configService.save(cfg);

        GenerationResult r = generatorService.generate("p1", false, false);
        assertTrue(r.failed() >= 1);
        assertEquals(0, r.generated());
    }

    @Test
    void targetExistsAsDirectoryCountsFailed() throws Exception {
        Path workspace = Files.createDirectory(tempDir.resolve("ws"));
        Path patternDir = Files.createDirectory(tempDir.resolve("pat"));
        Files.writeString(patternDir.resolve("foo.txt"), "a", StandardCharsets.UTF_8);

        ViracochaConfig cfg = new ViracochaConfig();
        cfg.getPatterns().add(new PatternEntry("pat1", patternDir.toString(), List.of()));
        cfg.getProjects().add(new ProjectEntry("p1", workspace.toString(),
            List.of(new MappingEntry("pat1", ".", new LinkedHashMap<>())), new LinkedHashMap<>()));
        configService.save(cfg);

        Files.createDirectory(workspace.resolve("foo.txt"));

        GenerationResult r = generatorService.generate("p1", false, false);
        assertTrue(r.failed() >= 1);
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
