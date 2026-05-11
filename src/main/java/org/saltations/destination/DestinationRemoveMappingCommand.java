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
 * Command: vira destination remove-mapping NAME INDEX
 * Removes a mapping from a destination by 0-based index.
 * D-22: success "Mapping <INDEX> removed from destination '<name>'."
 * D-23: dest not found returns false -> "Destination '<name>' not found." exit 1
 * D-24: index out of range throws IndexOutOfBoundsException -> exit 1
 */
@Command(
    name = "remove-mapping",
    description = "Remove a mapping from a destination by index.",
    mixinStandardHelpOptions = true
)
@Singleton
public class DestinationRemoveMappingCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", description = "Name of the destination.")
    private String destName;

    @Parameters(index = "1", description = "0-based index of the mapping to remove.")
    private int index;

    private final DestinationService destinationService;

    @Inject
    public DestinationRemoveMappingCommand(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @Override
    public Integer call() {
        try {
            boolean removed = destinationService.removeMapping(destName, index);
            if (!removed) {
                // false means destination not found (D-23)
                spec.commandLine().getErr().println("Destination '" + destName + "' not found.");
                return 1;
            }
            // D-22: "Mapping <INDEX> removed from destination '<name>'."
            spec.commandLine().getOut().println(
                "Mapping " + index + " removed from destination '" + destName + "'.");
            return 0;
        } catch (ConfigNotInitializedException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IndexOutOfBoundsException e) {
            // D-24: "Mapping index N out of range (destination has M mappings)."
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 1;
        }
    }
}
