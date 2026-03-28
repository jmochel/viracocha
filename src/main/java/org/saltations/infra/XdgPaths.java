package org.saltations.infra;

import jakarta.inject.Singleton;
import java.nio.file.Path;

/**
 * Resolves XDG Base Directory paths for viracocha.
 * Config: $XDG_CONFIG_HOME/viracocha/config.yaml (fallback: ~/.config/viracocha/config.yaml)
 * Log:    ~/.local/share/viracocha/vira.jsonl
 */
@Singleton
public class XdgPaths {

    public Path configDir() {
        return configFile().getParent();
    }

    public Path configFile() {
        String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        Path base = (xdgConfigHome != null && !xdgConfigHome.isBlank())
            ? Path.of(xdgConfigHome)
            : Path.of(System.getProperty("user.home"), ".config");
        return base.resolve("viracocha").resolve("config.yaml");
    }

    public Path dataDir() {
        return Path.of(System.getProperty("user.home"), ".local", "share", "viracocha");
    }

    public Path logFile() {
        return dataDir().resolve("vira.jsonl");
    }
}
