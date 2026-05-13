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
        var xdgPaths = new XdgPaths(tempDir.toAbsolutePath().toString());
        configService = new ConfigService(xdgPaths);
    }

    @Test
    void initCreatesConfigFile() throws IOException {
        var created = configService.init();
        assertTrue(Files.exists(created), "init() must create config file");
        String contents = Files.readString(created);
        assertTrue(contents.contains("version: 3"), "Config file must contain 'version: 3'");
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
        var config = configService.load();
        assertEquals(3, config.getVersion(), "Loaded config must have version=3");
    }

    @Test
    void saveAndLoadRoundTripPreservesVersion() throws IOException {
        configService.init();
        var config = configService.load();
        config.setVersion(42);
        configService.save(config);
        var reloaded = configService.load();
        assertEquals(42, reloaded.getVersion(), "save()+load() must preserve version field");
    }

    @Test
    void loadThrowsConfigVersionExceptionForV1Config() throws IOException {
        // Write a v1 config file manually (simulates a developer's existing v2 config)
        var configDir = configService.xdgPaths().configDir();
        Files.createDirectories(configDir);
        var configFile = configService.xdgPaths().configFile();
        Files.writeString(configFile,
            "version: 1\ncatalogs: []\narchetypes: []\nprojects: []\n");
        ConfigVersionException ex = assertThrows(ConfigVersionException.class,
            () -> configService.load(),
            "load() must throw ConfigVersionException when version is 1");
        assertTrue(ex.getMessage().contains("v3 format required"),
            "Exception message must contain 'v3 format required'");
        assertTrue(ex.getMessage().contains("v1"),
            "Exception message must report the found version (v1)");
    }

    @Test
    void loadThrowsConfigVersionExceptionForMissingVersionField() throws IOException {
        var configDir = configService.xdgPaths().configDir();
        Files.createDirectories(configDir);
        var configFile = configService.xdgPaths().configFile();
        Files.writeString(configFile, "catalogs: []\n"); // no version field
        ConfigVersionException ex = assertThrows(ConfigVersionException.class,
            () -> configService.load(),
            "load() must throw ConfigVersionException when version field is absent");
        assertTrue(ex.getMessage().contains("v0"),
            "Exception message must report version 0 for missing field");
    }

    @Test
    void loadSucceedsForV3Config() throws IOException {
        configService.init();
        // init() writes version:3; this must load without exception
        ViracochaConfig config = assertDoesNotThrow(() -> configService.load());
        assertEquals(3, config.getVersion());
    }

    @Test
    void initWritesVersion3() throws IOException {
        var path = configService.init();
        String contents = Files.readString(path);
        assertTrue(contents.contains("version: 3"),
            "init() must write a config file containing 'version: 3'");
    }
}
