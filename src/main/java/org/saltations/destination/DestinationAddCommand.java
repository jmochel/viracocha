package org.saltations.destination;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.model.DestinationEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira destination add --name NAME --path PATH
 * Registers a named destination workspace path.
 * Exit codes: 0 = success, 1 = validation failure / config error / IO error.
 * D-04: no path existence check — destinations may not exist at registration time.
 */
@Command(
    name = "add",
    description = "Register a named destination workspace path.",
    mixinStandardHelpOptions = true
)
@Singleton
public class DestinationAddCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = {"--name"}, required = true,
            description = "Unique name for this destination.")
    private String name;

    @Option(names = {"--path"}, required = true,
            description = "Absolute or relative path to the destination directory.")
    private String path;

    private final DestinationService destinationService;

    @Inject
    public DestinationAddCommand(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @Override
    public Integer call() {
        try {
            DestinationEntry entry = destinationService.addDestination(name, path);
            spec.commandLine().getOut().println("Destination '" + entry.getName() + "' added.");
            return 0;
        } catch (ConfigNotInitializedException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IllegalArgumentException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 1;
        }
    }
}
