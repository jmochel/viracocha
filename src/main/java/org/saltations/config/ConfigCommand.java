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
    aliases = {"cfg"},
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
        return 0;
    }
}
