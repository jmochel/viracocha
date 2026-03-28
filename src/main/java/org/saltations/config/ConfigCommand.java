package org.saltations.config;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Group command: vira config
 * Provides subcommands for managing the central config file.
 */
@Command(
    name = "config",
    description = "Manage the viracocha central configuration.",
    mixinStandardHelpOptions = true,
    subcommands = {
        InitCommand.class,
        ShowConfigCommand.class
    }
)
@Singleton
public class ConfigCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        // Group command with no subcommand: show help
        return 0;
    }
}
