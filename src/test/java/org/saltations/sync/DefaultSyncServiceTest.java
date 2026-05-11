package org.saltations.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.destination.DestinationService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.infra.XdgPaths;
import org.saltations.source.SourceService;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stub tests for DefaultSyncService — all disabled as stubs.
 * Wave 2 (Plan 02) enables each test with real assertions.
 * Covers SYN-01 and SYN-02.
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
        syncService = new DefaultSyncService(configService);
    }

    // --- SYN-01: sync copies changed files to destination ---

    @Disabled
    @Test
    void syncCopiesChangedFilesToDestination() throws Exception {
    }

    // --- SYN-01: sync skips content-identical files ---

    @Disabled
    @Test
    void syncSkipsContentIdenticalFiles() throws Exception {
    }

    // --- SYN-01: sync ignores non-sync mappings ---

    @Disabled
    @Test
    void syncIgnoresNonSyncMappings() throws Exception {
    }

    // --- SYN-01: sync skips template sources ---

    @Disabled
    @Test
    void syncSkipsTemplateSources() throws Exception {
    }

    // --- SYN-02: sync detects conflict when dest newer ---

    @Disabled
    @Test
    void syncDetectsConflictWhenDestNewer() throws Exception {
    }

    // --- SYN-02: sync no conflict when content identical ---

    @Disabled
    @Test
    void syncNoConflictWhenContentIdentical() throws Exception {
    }
}
