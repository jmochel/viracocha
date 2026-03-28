package org.saltations.config;

/**
 * Thrown by ConfigService.load() when the central config file does not exist.
 * Commands that require config should catch this and print:
 * "Config not initialized. Run 'vira config init' first." then return exit code 1.
 */
public class ConfigNotInitializedException extends RuntimeException {

    public ConfigNotInitializedException() {
        super("Config not initialized. Run 'vira config init' first.");
    }
}
