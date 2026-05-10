package org.saltations.destination;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.model.DestinationEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command: vira destination list [--json]
 * Lists all registered destinations.
 * Plain output — name + path, aligned columns, no header.
 * --json switches to JSONL (one JSON object per line).
 * Exit codes: 0 = success (including empty list), 1 = config error / IO error.
 */
@Command(
    name = "list",
    description = "List all registered destinations.",
    mixinStandardHelpOptions = true
)
@Singleton
public class DestinationListCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = {"--json"},
            description = "Output as JSONL (one JSON object per line).")
    private boolean json;

    private final DestinationService destinationService;

    @Inject
    public DestinationListCommand(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @Override
    public Integer call() {
        try {
            List<DestinationEntry> destinations = destinationService.listDestinations();
            PrintWriter out = spec.commandLine().getOut();
            if (json) {
                ObjectMapper om = new ObjectMapper();
                for (DestinationEntry e : destinations) {
                    out.println(om.writeValueAsString(e));
                }
            } else {
                // name + path, aligned columns, no header
                int maxNameWidth = destinations.stream()
                    .mapToInt(e -> e.getName().length())
                    .max().orElse(0);
                for (DestinationEntry e : destinations) {
                    out.println(String.format("%-" + maxNameWidth + "s  %s",
                        e.getName(), e.getPath()));
                }
            }
            out.flush();
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
