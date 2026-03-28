package org.saltations.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.infra.XdgPaths;
import org.saltations.model.ViracochaConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigService.
 * Uses a @TempDir-backed XdgPaths stub to avoid touching real XDG config.
 */
class ConfigServiceTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;

    @BeforeEach
    void setUp() {
        // Inline stub: override configFile() to point into @TempDir
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
    }

    @Test
    void initCreatesConfigFile() throws IOException {
        Path created = configService.init();
        assertTrue(Files.exists(created), "init() must create config file");
        String contents = Files.readString(created);
        assertTrue(contents.contains("version: 1"), "Config file must contain 'version: 1'");
    }

    @Test
    void initIsIdempotent() throws IOException {
        configService.init();
        String firstContents = Files.readString(configService.init());
        configService.init(); // second call
        String secondContents = Files.readString(configService.init());
        // Content must not change on repeated calls
        assertEquals(firstContents.trim(), secondContents.trim(),
            "Repeated init() must not change existing config contents");
    }

    @Test
    void loadThrowsWhenConfigMissing() {
        assertThrows(ConfigNotInitializedException.class,
            () -> configService.load(),
            "load() must throw ConfigNotInitializedException when config file does not exist");
    }

    @Test
    void loadAfterInitReturnsDefaultConfig() throws IOException {
        configService.init();
        ViracochaConfig config = configService.load();
        assertEquals(1, config.getVersion(), "Loaded config must have version=1");
    }

    @Test
    void saveAndLoadRoundTripPreservesVersion() throws IOException {
        configService.init();
        ViracochaConfig config = configService.load();
        config.setVersion(42);
        configService.save(config);
        ViracochaConfig reloaded = configService.load();
        assertEquals(42, reloaded.getVersion(), "save()+load() must preserve version field");
    }
}
