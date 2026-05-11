package org.saltations.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.infra.XdgPaths;
import org.saltations.model.ViracochaConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages the central viracocha config file (config.yaml).
 * Load fresh on each command invocation — do not cache across calls.
 */
@Singleton
public class ConfigService {

    private final XdgPaths xdgPaths;
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    @Inject
    public ConfigService(XdgPaths xdgPaths) {
        this.xdgPaths = xdgPaths;
    }

    /**
     * Creates the XDG config directory and an empty config.yaml if one does not exist.
     * Idempotent — safe to call repeatedly.
     *
     * @return path to the config file
     * @throws IOException if directory creation or file write fails
     */
    public Path init() throws IOException {
        Path configDir = xdgPaths.configDir();
        Path configFile = xdgPaths.configFile();
        Files.createDirectories(configDir);
        // Also ensure the log data directory exists for logback
        Files.createDirectories(xdgPaths.dataDir());
        if (!Files.exists(configFile)) {
            save(new ViracochaConfig());
        }
        return configFile;
    }

    /**
     * Loads config.yaml from the XDG config path.
     *
     * @throws ConfigNotInitializedException if config file does not exist
     * @throws ConfigVersionException if the config file version is less than 3
     * @throws IOException if file cannot be read or parsed
     */
    public ViracochaConfig load() throws IOException {
        Path configFile = xdgPaths.configFile();
        if (!Files.exists(configFile)) {
            throw new ConfigNotInitializedException();
        }
        // Version pre-read: detect v2 config before full deserialization (per D-09)
        JsonNode root = yaml.readTree(configFile.toFile());
        JsonNode versionNode = root.get("version");
        int version = (versionNode == null || versionNode.isNull()) ? 0 : versionNode.asInt(0);
        if (version < 3) {
            throw new ConfigVersionException(version);
        }
        return yaml.readValue(configFile.toFile(), ViracochaConfig.class);
    }

    /**
     * Serializes the config to config.yaml, overwriting previous content.
     *
     * @throws IOException if file cannot be written
     */
    public void save(ViracochaConfig config) throws IOException {
        yaml.writeValue(xdgPaths.configFile().toFile(), config);
    }

    /**
     * Exposes the underlying XdgPaths so commands can check file existence
     * before calling init().
     */
    public XdgPaths xdgPaths() {
        return xdgPaths;
    }
}
