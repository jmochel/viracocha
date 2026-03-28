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
 * Command: vira config show
 * Displays the config file path and its raw YAML contents.
 * Output format:
 *   Config file: &lt;absolute-path&gt;
 *   &lt;blank line&gt;
 *   &lt;raw YAML contents&gt;
 * Exit codes: 0 = success, 1 = config not initialized or IO error
 */
@Command(
    name = "show",
    description = "Display the current configuration file path and contents.",
    mixinStandardHelpOptions = true
)
@Singleton
public class ShowConfigCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    private final ConfigService configService;

    @Inject
    public ShowConfigCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            // load() throws ConfigNotInitializedException if config missing
            configService.load();
            java.nio.file.Path configFile = configService.xdgPaths().configFile();
            String rawYaml = Files.readString(configFile);
            spec.commandLine().getOut().println("Config file: " + configFile.toAbsolutePath());
            spec.commandLine().getOut().println();
            spec.commandLine().getOut().println(rawYaml);
            spec.commandLine().getOut().flush();
            return 0;
        } catch (ConfigNotInitializedException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Error reading config: " + e.getMessage());
            return 1;
        }
    }
}
