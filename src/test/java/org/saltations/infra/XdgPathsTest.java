package org.saltations.infra;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for XdgPaths. Uses plain JUnit (no Micronaut context needed).
 * Cannot set environment variables in-process; tests verify the fallback path logic
 * by checking the default behavior when XDG_CONFIG_HOME is unset in the test JVM.
 */
class XdgPathsTest {

    private final XdgPaths xdgPaths = new XdgPaths();

    @Test
    void configFileEndsWithExpectedSegments() {
        Path configFile = xdgPaths.configFile();
        assertTrue(configFile.toString().endsWith("viracocha/config.yaml"),
            "configFile() must end with viracocha/config.yaml, got: " + configFile);
    }

    @Test
    void configDirIsParentOfConfigFile() {
        assertEquals(xdgPaths.configFile().getParent(), xdgPaths.configDir(),
            "configDir() must be parent of configFile()");
    }

    @Test
    void logFileEndsWithExpectedSegments() {
        Path logFile = xdgPaths.logFile();
        assertTrue(logFile.toString().endsWith("viracocha/vira.jsonl"),
            "logFile() must end with viracocha/vira.jsonl, got: " + logFile);
    }

    @Test
    void configFileUsesHomeDotConfigWhenXdgUnset() {
        // When XDG_CONFIG_HOME is not set (the common case in test JVM),
        // path must be under ~/.config/
        if (System.getenv("XDG_CONFIG_HOME") == null) {
            String home = System.getProperty("user.home");
            assertTrue(xdgPaths.configFile().startsWith(Path.of(home, ".config")),
                "With no XDG_CONFIG_HOME, must be under ~/.config");
        }
    }
}
