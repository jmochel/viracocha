package org.saltations.destination;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.model.DestinationEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command: vira destination list
 * Lists all registered destinations.
 * Plain output — name + path, aligned columns, no header.
 * Exit codes: 0 = success (including empty list), 1 = config error / IO error.
 */
@Command(
    name = "list",
    aliases = {"ls"},
    description = "List all registered destinations.",
    mixinStandardHelpOptions = true
)
@Singleton
public class DestinationListCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    private final DestinationService destinationService;

    @Inject
    public DestinationListCommand(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @Override
    public Integer call() {
        try {
            List<DestinationEntry> destinations = destinationService.listDestinations();
            var out = spec.commandLine().getOut();
            
        // name + path, aligned columns, no header
            var maxNameWidth = destinations.stream()
                .mapToInt(e -> e.getName().length())
                .max().orElse(0);
            for (DestinationEntry e : destinations) {
                out.println(String.format("%-" + maxNameWidth + "s  %s",
                    e.getName(), e.getPath()));
            }
            
            out.flush();
            return 0;
        } catch (ConfigNotInitializedException | IOException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        }
   
    }
}
