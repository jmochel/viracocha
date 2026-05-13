package org.saltations.destination;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira destination add --name NAME --path PATH
 * Registers a named destination workspace path.
 * Exit codes: 0 = success, 1 = validation failure, configuration error, or I/O error.
 * Note: This command does not verify whether the specified path exists; 
 * destinations can be registered even if the directory does not currently exist so that they can be generated.
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

    @Option(names = {"-n", "--name"}, required = true,
            description = "Unique name for this destination.")
    private String name;

    @Option(names = {"-p", "--path"}, required = true,
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
            var entry = destinationService.addDestination(name, path);
            spec.commandLine().getOut().println("Destination '" + entry.getName() + "' added.");
            return 0;
        } catch (ConfigNotInitializedException | IllegalArgumentException |IOException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        }
    }
}
