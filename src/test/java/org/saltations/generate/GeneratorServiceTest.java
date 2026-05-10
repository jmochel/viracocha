package org.saltations.generate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.destination.DestinationService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.infra.XdgPaths;
import org.saltations.source.SourceService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test scaffold for GeneratorService — Wave 0 test infrastructure.
 * Tests are @Disabled until Wave 1 (Plan 01) implements GeneratorService.
 * Uses plain JUnit 5 with inline XdgPaths stub and @TempDir isolation.
 * No @MicronautTest — mirrors DestinationServiceTest pattern.
 * Covers GEN-01 through GEN-04.
 */
class GeneratorServiceTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private SourceService sourceService;
    private DestinationService destinationService;
    private PathExpander pathExpander;
    private GeneratorService generatorService;

    @BeforeEach
    void setUp() throws Exception {
        XdgPaths xdgPaths = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
        };
        configService = new ConfigService(xdgPaths);
        configService.init();
        FreemarkerVariableExtractor extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
        destinationService = new DestinationService(configService);
        pathExpander = new PathExpander();
        generatorService = new GeneratorService(configService, pathExpander);
    }

    // --- GEN-01: flat copy ---

    @Disabled("Wave 1: implement GeneratorService first")
    @Test
    void generateFlatCopyWritesFilesToDestination() throws Exception {
        // Set up source directory with two text files
        Path sourceDir = Files.createDirectory(tempDir.resolve("source-flat"));
        Files.writeString(sourceDir.resolve("file1.txt"), "content1");
        Files.writeString(sourceDir.resolve("file2.txt"), "content2");

        // Register source (templates: false) and destination
        sourceService.addSource("flat-src", sourceDir.toString(), false);
        Path destDir = Files.createDirectory(tempDir.resolve("dest-flat"));
        destinationService.addDestination("flat-dest", destDir.toString());
        destinationService.addMapping("flat-dest", "flat-src", null, false, false);

        // Run generate
        GenerationResult result = generatorService.generate("flat-dest", false, false);

        // Assert both files exist in destination
        assertTrue(Files.exists(destDir.resolve("file1.txt")), "file1.txt should exist in destination");
        assertTrue(Files.exists(destDir.resolve("file2.txt")), "file2.txt should exist in destination");
        assertEquals(2, result.generated(), "Should report 2 generated files");
    }

    // --- GEN-01: recursive walk ---

    @Disabled("Wave 1: implement GeneratorService first")
    @Test
    void generateRecursiveCopyWalksSubdirectories() throws Exception {
        // Set up source with subdirectory
        Path sourceDir = Files.createDirectory(tempDir.resolve("source-recurse"));
        Path subDir = Files.createDirectory(sourceDir.resolve("subdir"));
        Files.writeString(subDir.resolve("deep.txt"), "deep content");

        // Register source + destination + mapping (recurse: true)
        sourceService.addSource("recurse-src", sourceDir.toString(), false);
        Path destDir = Files.createDirectory(tempDir.resolve("dest-recurse"));
        destinationService.addDestination("recurse-dest", destDir.toString());
        destinationService.addMapping("recurse-dest", "recurse-src", null, true, false);

        // Run generate
        generatorService.generate("recurse-dest", false, false);

        // Assert subdirectory file exists in destination
        assertTrue(Files.exists(destDir.resolve("subdir").resolve("deep.txt")),
            "destDir/subdir/deep.txt should exist after recursive copy");
    }

    // --- GEN-02: skip-existing ---

    @Disabled("Wave 1: implement GeneratorService first")
    @Test
    void generateSkipsExistingDestinationFiles() throws Exception {
        // Set up source and destination
        Path sourceDir = Files.createDirectory(tempDir.resolve("source-skip"));
        Files.writeString(sourceDir.resolve("existing.txt"), "original content");

        sourceService.addSource("skip-src", sourceDir.toString(), false);
        Path destDir = Files.createDirectory(tempDir.resolve("dest-skip"));
        destinationService.addDestination("skip-dest", destDir.toString());
        destinationService.addMapping("skip-dest", "skip-src", null, false, false);

        // Generate once — writes file
        generatorService.generate("skip-dest", false, false);

        // Overwrite source with different content
        Files.writeString(sourceDir.resolve("existing.txt"), "modified content");

        // Generate again
        GenerationResult secondResult = generatorService.generate("skip-dest", false, false);

        // Destination file should still have original content (skip-existing)
        String destContent = Files.readString(destDir.resolve("existing.txt"));
        assertEquals("original content", destContent, "Existing destination file must not be overwritten");
        assertEquals(1, secondResult.skipped(), "Second run should report 1 skipped file");
    }

    // --- GEN-01: glob filter ---

    @Disabled("Wave 1: implement GeneratorService first")
    @Test
    void generateWithGlobFilterSelectsMatchingFiles() throws Exception {
        // Set up source with mixed file types
        Path sourceDir = Files.createDirectory(tempDir.resolve("source-glob"));
        Files.writeString(sourceDir.resolve("readme.md"), "# readme");
        Files.writeString(sourceDir.resolve("config.yaml"), "key: value");

        sourceService.addSource("glob-src", sourceDir.toString(), false);
        Path destDir = Files.createDirectory(tempDir.resolve("dest-glob"));
        destinationService.addDestination("glob-dest", destDir.toString());
        // glob: only *.md files
        destinationService.addMapping("glob-dest", "glob-src", "*.md", false, false);

        generatorService.generate("glob-dest", false, false);

        // Only readme.md should be in destination
        assertTrue(Files.exists(destDir.resolve("readme.md")), "readme.md should be copied");
        assertFalse(Files.exists(destDir.resolve("config.yaml")), "config.yaml should be filtered out by glob");
    }

    // --- GEN-01/D-04: hidden path skipping ---

    @Disabled("Wave 1: implement GeneratorService first")
    @Test
    void generateSkipsHiddenPathSegments() throws Exception {
        // Set up source with hidden directory and normal file
        Path sourceDir = Files.createDirectory(tempDir.resolve("source-hidden"));
        Path gitDir = Files.createDirectory(sourceDir.resolve(".git"));
        Files.writeString(gitDir.resolve("config"), "git config content");
        Files.writeString(sourceDir.resolve("real.txt"), "real content");

        sourceService.addSource("hidden-src", sourceDir.toString(), false);
        Path destDir = Files.createDirectory(tempDir.resolve("dest-hidden"));
        destinationService.addDestination("hidden-dest", destDir.toString());
        destinationService.addMapping("hidden-dest", "hidden-src", null, true, false);

        generatorService.generate("hidden-dest", false, false);

        // real.txt should be copied, .git directory should be skipped
        assertTrue(Files.exists(destDir.resolve("real.txt")), "real.txt should be copied");
        assertFalse(Files.exists(destDir.resolve(".git")), ".git directory should be skipped (hidden)");
    }

    // --- GEN-03: template expansion in path and content ---

    @Disabled("Wave 1: implement GeneratorService first")
    @Test
    void generateTemplateSourceExpandsPathSegmentsAndContent() throws Exception {
        // Set up source with Freemarker template in filename and content
        Path sourceDir = Files.createDirectory(tempDir.resolve("source-template"));
        Files.writeString(sourceDir.resolve("${project}.txt"), "Hello ${name}!");

        // Register as template source (templates: true)
        sourceService.addSource("tmpl-src", sourceDir.toString(), true);
        Path destDir = Files.createDirectory(tempDir.resolve("dest-template"));
        destinationService.addDestination("tmpl-dest", destDir.toString());
        // Add mapping with parameters: project=myproj, name=world
        destinationService.addMapping("tmpl-dest", "tmpl-src", null, false, false);
        // Set parameters on mapping (index 0): project -> myproj, name -> world
        // (This requires the mapping to support parameter values — wire via config)

        generatorService.generate("tmpl-dest", false, false);

        // destDir/myproj.txt should exist with expanded content
        Path expandedFile = destDir.resolve("myproj.txt");
        assertTrue(Files.exists(expandedFile), "Expanded file myproj.txt should exist");
        assertEquals("Hello world!", Files.readString(expandedFile), "Template content should be expanded");
    }

    // --- GEN-04: binary byte integrity ---

    @Disabled("Wave 1: implement GeneratorService first")
    @Test
    void generateBinarySourceByteCopiesToDestination() throws Exception {
        // Write known non-UTF8 bytes to source
        byte[] expectedBytes = {0x00, (byte) 0xFF, 0x1A, 0x2B, 0x3C};
        Path sourceDir = Files.createDirectory(tempDir.resolve("source-binary"));
        Files.write(sourceDir.resolve("sample.bin"), expectedBytes);

        sourceService.addSource("bin-src", sourceDir.toString(), false);
        Path destDir = Files.createDirectory(tempDir.resolve("dest-binary"));
        destinationService.addDestination("bin-dest", destDir.toString());
        destinationService.addMapping("bin-dest", "bin-src", null, false, false);

        generatorService.generate("bin-dest", false, false);

        // Read destination bytes and assert exact byte equality
        byte[] actualBytes = Files.readAllBytes(destDir.resolve("sample.bin"));
        assertArrayEquals(expectedBytes, actualBytes, "Binary file must be copied with exact byte integrity");
    }

    // --- Pitfall 6: destination not found ---

    @Disabled("Wave 1: implement GeneratorService first")
    @Test
    void generateDestinationNotFoundThrowsIllegalArgumentException() throws Exception {
        // Call generate with a destination name that does not exist in config
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> generatorService.generate("nonexistent-dest", false, false));
        assertTrue(ex.getMessage().contains("nonexistent-dest"),
            "Exception message should contain the unknown destination name");
    }
}
