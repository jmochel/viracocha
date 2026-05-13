package org.saltations.config;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;

/**
 * Command: vira config init
 * Creates the XDG config directory and an empty config.yaml.
 * Idempotent — safe to run repeatedly.
 * Exit codes: 0 = success (new or existing), 1 = IO error
 */
@Command(
    name = "init",
    description = "Initialize the viracocha configuration file.",
    mixinStandardHelpOptions = true
)
@Singleton
public class InitCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    private final ConfigService configService;

    @Inject
    public InitCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var configFile = configService.xdgPaths().configFile();
            var alreadyExisted = Files.exists(configFile);
            configService.init();
            if (alreadyExisted) {
                spec.commandLine().getOut().println("Config already initialized at " + configFile.toAbsolutePath());
            } else {
                spec.commandLine().getOut().println("Config initialized at " + configFile.toAbsolutePath());
            }
            return 0;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Error initializing config: " + e.getMessage());
            return 1;
        }
    }
}
