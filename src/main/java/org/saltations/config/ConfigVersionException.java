package org.saltations.config;

import java.io.IOException;

/**
 * Thrown by ConfigService.load() when the config file version is less than 3.
 * Extends IOException so it is caught by existing IOException handlers in commands
 * (e.g. ShowConfigCommand) and the message propagates to stderr as-is.
 * Commands must not catch this separately -- the IOException handler covers it.
 */
public class ConfigVersionException extends IOException {

    public ConfigVersionException(int foundVersion) {
        super("Config file is v" + foundVersion
            + " — v3 format required."
            + " Delete ~/.config/viracocha/config.yaml"
            + " and run 'vira config init' to start fresh.");
    }
}
