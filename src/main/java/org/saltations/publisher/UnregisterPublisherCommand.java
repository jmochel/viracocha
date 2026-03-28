package org.saltations.publisher;

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
 * Command: vira publisher unregister
 * Removes a named publisher from central config.
 * Per D-07: prints error and exits 1 if name is not found.
 * Exit codes: 0 = success, 1 = not found or any error
 */
@Command(name = "unregister", description = "Unregister a named publisher.", mixinStandardHelpOptions = true)
@Singleton
public class UnregisterPublisherCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--name"}, required = true, description = "Publisher name to remove")
    private String name;

    private final ConfigService configService;

    @Inject
    public UnregisterPublisherCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var config = configService.load();
            boolean removed = config.getPublishers().removeIf(e -> e.getName().equals(name));
            if (!removed) {
                spec.commandLine().getErr().println("Publisher '" + name + "' not found.");
                return 1;
            }
            configService.save(config);
            spec.commandLine().getOut().println("Publisher '" + name + "' unregistered.");
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
