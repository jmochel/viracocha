package org.saltations.destination;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira destination remove NAME
 * Removes a named destination from the config.
 * D-16: exits 1 with "Destination 'NAME' not found." if name is not registered.
 * Exit codes: 0 = success, 1 = not found / config error / IO error.
 */
@Command(
    name = "remove",
    aliases = {"rm"},
    description = "Remove a named destination from the configuration.",
    mixinStandardHelpOptions = true
)
@Singleton
public class DestinationRemoveCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", description = "Name of the destination to remove.")
    private String name;

    private final DestinationService destinationService;

    @Inject
    public DestinationRemoveCommand(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @Override
    public Integer call() {
        try {
            var removed = destinationService.removeDestination(name);
            if (!removed) {
                spec.commandLine().getErr().println("Destination '" + name + "' not found.");
                return 1;
            }
            spec.commandLine().getOut().println("Destination '" + name + "' removed.");
            return 0;
        } catch (ConfigNotInitializedException | IOException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        }
   
    }
}
