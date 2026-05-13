package org.saltations.infra;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

import io.micronaut.context.annotation.Property;

/**
 * Resolves XDG Base Directory paths for viracocha.
 * Config: $XDG_CONFIG_HOME/viracocha/config.yaml (fallback: ~/.config/viracocha/config.yaml)
 * Log:    ~/.local/share/viracocha/vira.jsonl
 */

@Slf4j
@Singleton
public class XdgPaths {

    private static final String USER_HOME_PROPERTY = "user.home";
    private static final String LOCAL = ".local";
    private static final String SHARE = "share";
    private static final String APP_NAME = "viracocha";
    private static final String DEFAULT_CONFIG_FOLDER_NAME = ".config";
    private static final String CONFIG_FILE = "config.yaml";
    private static final String LOG_FILE = "vira.jsonl";
   
    private Path configFolder;
    private Path dataFolder;

    @Inject
    public XdgPaths(
            @Property(name = "xdg.config.home", defaultValue = "") String xdgConfigEnv
    ) 
    {
        this.configFolder = resolveToConfigFolder(xdgConfigEnv);
        this.dataFolder = resolveToDataFolder();
    }

    private static Path resolveToDataFolder() {
        return Path.of(System.getProperty(USER_HOME_PROPERTY), LOCAL, SHARE, APP_NAME);
    }

    private static Path resolveToConfigFolder(String suppliedEnvValue) {

        // Supplied XDG_CONFIG_HOME environment variable overrides default folder
        if (suppliedEnvValue != null && !suppliedEnvValue.isBlank()) {
            return Path.of(suppliedEnvValue).toAbsolutePath().normalize();
        }

        return Path.of(System.getProperty(USER_HOME_PROPERTY), DEFAULT_CONFIG_FOLDER_NAME, APP_NAME)
                   .toAbsolutePath()
                   .normalize();
    }

    public Path configDir() {
        return configFolder;
    }

    public Path configFile() {
        return configFolder.resolve(CONFIG_FILE);
    }

    public Path dataDir() {
        return dataFolder;
    }

    public Path logFile() {
        return dataDir().resolve(LOG_FILE);
    }
}
