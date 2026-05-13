package org.saltations.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.destination.DestinationService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.infra.XdgPaths;
import org.saltations.source.SourceService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for DefaultSyncService — all enabled with real assertions.
 * Wave 2 (Plan 02) implementation covering SYN-01 and SYN-02.
 */
class DefaultSyncServiceTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private SourceService sourceService;
    private DestinationService destinationService;
    private DefaultSyncService syncService;

    @BeforeEach
    void setUp() throws Exception {
        var xdgPaths = new XdgPaths("") {
            @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
        };
        configService = new ConfigService(xdgPaths);
        configService.init();
        var extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
        destinationService = new DestinationService(configService);
        syncService = new DefaultSyncService(configService);
    }

    // --- SYN-01: sync copies changed files to destination ---

    @Test
    void syncCopiesChangedFilesToDestination() throws Exception {
        // Set up source and dest directories
        var sourceDir = tempDir.resolve("copy-src-dir");
        var destDir = tempDir.resolve("copy-dest-dir");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Register source, destination, and sync mapping
        sourceService.addSource("copy-src", sourceDir.toString(), false);
        destinationService.addDestination("copy-dest", destDir.toString());
        destinationService.addMapping("copy-dest", "copy-src", null, false, true);

        // Write "original" to source and destination (simulate prior generate)
        Files.writeString(sourceDir.resolve("file.txt"), "original");
        Files.writeString(destDir.resolve("file.txt"), "original");

        // Advance source mtime so source is newer than dest
        Files.setLastModifiedTime(sourceDir.resolve("file.txt"),
            FileTime.from(Instant.now().plusSeconds(5)));

        // Now update source content to "updated"
        Files.writeString(sourceDir.resolve("file.txt"), "updated");
        Files.setLastModifiedTime(sourceDir.resolve("file.txt"),
            FileTime.from(Instant.now().plusSeconds(10)));

        var result = syncService.sync("copy-dest", false, false);

        assertEquals(1, result.copied(), "Should report 1 copied");
        assertEquals(0, result.conflicts(), "Should report 0 conflicts");
        assertEquals("updated", Files.readString(destDir.resolve("file.txt")),
            "Destination content should match updated source");
    }

    // --- SYN-01: sync skips content-identical files ---

    @Test
    void syncSkipsContentIdenticalFiles() throws Exception {
        var sourceDir = tempDir.resolve("identical-src-dir");
        var destDir = tempDir.resolve("identical-dest-dir");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        sourceService.addSource("identical-src", sourceDir.toString(), false);
        destinationService.addDestination("identical-dest", destDir.toString());
        destinationService.addMapping("identical-dest", "identical-src", null, false, true);

        // Write identical content to both
        Files.writeString(sourceDir.resolve("file.txt"), "same content");
        Files.writeString(destDir.resolve("file.txt"), "same content");

        // Source is newer in mtime (but content is same)
        Files.setLastModifiedTime(sourceDir.resolve("file.txt"),
            FileTime.from(Instant.now().plusSeconds(5)));

        var result = syncService.sync("identical-dest", false, false);

        assertEquals(1, result.skipped(), "Should report 1 skipped");
        assertEquals(0, result.copied(), "Should report 0 copied");
        assertEquals(0, result.conflicts(), "Should report 0 conflicts");
    }

    // --- SYN-01: sync ignores non-sync mappings ---

    @Test
    void syncIgnoresNonSyncMappings() throws Exception {
        var sourceDir = tempDir.resolve("nosync-src-dir");
        var destDir = tempDir.resolve("nosync-dest-dir");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        Files.writeString(sourceDir.resolve("file.txt"), "source content");

        sourceService.addSource("nosync-src", sourceDir.toString(), false);
        destinationService.addDestination("nosync-dest", destDir.toString());
        // sync=false: mapping should be ignored
        destinationService.addMapping("nosync-dest", "nosync-src", null, false, false);

        var result = syncService.sync("nosync-dest", false, false);

        assertEquals(0, result.copied(), "Should report 0 copied for non-sync mapping");
        assertEquals(0, result.skipped(), "Should report 0 skipped for non-sync mapping");
        assertEquals(0, result.conflicts(), "Should report 0 conflicts for non-sync mapping");
        assertFalse(Files.exists(destDir.resolve("file.txt")),
            "Destination file should not be created for non-sync mapping");
    }

    // --- SYN-01: sync skips template sources ---

    @Test
    void syncSkipsTemplateSources() throws Exception {
        var sourceDir = tempDir.resolve("tmpl-src-dir");
        var destDir = tempDir.resolve("tmpl-dest-dir");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        Files.writeString(sourceDir.resolve("file.txt"), "${variable}");

        // templates=true: source is a template source, should be silently skipped (D-04)
        sourceService.addSource("tmpl-src", sourceDir.toString(), true);
        destinationService.addDestination("tmpl-dest", destDir.toString());
        // sync=true: mapping wants to sync, but source.templates=true should block it
        destinationService.addMapping("tmpl-dest", "tmpl-src", null, false, true);

        var result = syncService.sync("tmpl-dest", false, false);

        assertEquals(0, result.copied(), "Should report 0 copied for template source");
        assertEquals(0, result.conflicts(), "Should report 0 conflicts for template source");
        assertFalse(Files.exists(destDir.resolve("file.txt")),
            "Template file should not be copied to destination");
    }

    // --- SYN-02: sync detects conflict when dest newer ---

    @Test
    void syncDetectsConflictWhenDestNewer() throws Exception {
        var sourceDir = tempDir.resolve("conflict-src-dir");
        var destDir = tempDir.resolve("conflict-dest-dir");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Write different content to source and dest
        Files.writeString(sourceDir.resolve("file.txt"), "source version");
        Files.writeString(destDir.resolve("file.txt"), "local changes");

        // Dest is newer (simulating local modification after last sync)
        Files.setLastModifiedTime(destDir.resolve("file.txt"),
            FileTime.from(Instant.now().plusSeconds(5)));

        sourceService.addSource("conflict-src", sourceDir.toString(), false);
        destinationService.addDestination("conflict-dest", destDir.toString());
        destinationService.addMapping("conflict-dest", "conflict-src", null, false, true);

        var result = syncService.sync("conflict-dest", false, false);

        assertEquals(1, result.conflicts(), "Should report 1 conflict");
        assertFalse(result.conflictRecords().isEmpty(), "conflictRecords should be non-empty");
        assertEquals(SyncConflictKind.CONTENT_MISMATCH, result.conflictRecords().get(0).getKind(),
            "Conflict kind should be CONTENT_MISMATCH");
    }

    // --- SYN-02: sync no conflict when content identical ---

    @Test
    void syncNoConflictWhenContentIdentical() throws Exception {
        var sourceDir = tempDir.resolve("noconflict-src-dir");
        var destDir = tempDir.resolve("noconflict-dest-dir");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destDir);

        // Write identical content to source and dest
        Files.writeString(sourceDir.resolve("file.txt"), "same");
        Files.writeString(destDir.resolve("file.txt"), "same");

        // Dest is newer (but content is same — should not be flagged as conflict per D-03)
        Files.setLastModifiedTime(destDir.resolve("file.txt"),
            FileTime.from(Instant.now().plusSeconds(5)));

        sourceService.addSource("noconflict-src", sourceDir.toString(), false);
        destinationService.addDestination("noconflict-dest", destDir.toString());
        destinationService.addMapping("noconflict-dest", "noconflict-src", null, false, true);

        var result = syncService.sync("noconflict-dest", false, false);

        assertEquals(0, result.conflicts(), "Should report 0 conflicts when content identical");
        assertEquals(1, result.skipped(), "Should report 1 skipped when content identical");
    }
}
