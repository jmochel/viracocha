package org.saltations.pattern;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira pattern unregister
 * Per D-07: prints error and exits 1 if pattern name is not found.
 * Exit codes: 0 = success, 1 = not found or any error
 */
@Command(name = "unregister", description = "Unregister a named pattern.", mixinStandardHelpOptions = true)
@Singleton
public class UnregisterPatternCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"-n", "--name"}, required = true, description = "Pattern name to remove")
    private String name;

    private final ConfigService configService;

    @Inject
    public UnregisterPatternCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var config = configService.load();
            boolean removed = config.getPatterns().removeIf(e -> e.getName().equals(name));
            if (!removed) {
                spec.commandLine().getErr().println("Pattern '" + name + "' not found.");
                return 1;
            }
            configService.save(config);
            spec.commandLine().getOut().println("Pattern '" + name + "' unregistered.");
            return 0;
        } catch (ConfigNotInitializedException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 1;
        }
    }
}
