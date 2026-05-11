package org.saltations.destination;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.model.DestinationEntry;
import org.saltations.model.MappingEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Command: vira destination show NAME [--json]
 * Displays full details for a named destination including parameters and mappings.
 * D-11: Parameters block OMITTED when map is empty.
 * D-12: Mappings section always shown; "Mappings: (none)" when empty.
 * D-13: null glob displayed as "(all files)".
 * D-07: --json outputs a single JSON object.
 * Exit codes: 0 = success, 1 = not found / config error / IO error.
 */
@Command(
    name = "show",
    description = "Display full details for a named destination.",
    mixinStandardHelpOptions = true
)
@Singleton
public class DestinationShowCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", description = "Name of the destination to display.")
    private String name;

    @Option(names = {"--json"},
            description = "Output as a single JSON object.")
    private boolean json;

    private final DestinationService destinationService;

    @Inject
    public DestinationShowCommand(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @Override
    public Integer call() {
        try {
            Optional<DestinationEntry> result = destinationService.getDestination(name);
            if (result.isEmpty()) {
                spec.commandLine().getErr().println("Destination '" + name + "' not found.");
                return 1;
            }
            DestinationEntry entry = result.get();
            PrintWriter out = spec.commandLine().getOut();
            if (json) {
                // D-07: single JSON object
                ObjectMapper om = new ObjectMapper();
                out.println(om.writeValueAsString(entry));
            } else {
                out.println("Name:      " + entry.getName());
                out.println("Path:      " + entry.getPath());
                // D-11: Parameters block ONLY when map is non-empty
                if (!entry.getParameters().isEmpty()) {
                    out.println("Parameters:");
                    for (Map.Entry<String, String> e : entry.getParameters().entrySet()) {
                        out.println("  " + e.getKey() + ": " + e.getValue());
                    }
                }
                // D-12: Always show Mappings section
                if (entry.getMappings().isEmpty()) {
                    out.println("Mappings: (none)");
                } else {
                    for (int i = 0; i < entry.getMappings().size(); i++) {
                        MappingEntry m = entry.getMappings().get(i);
                        out.println("Mapping " + (i + 1) + ":");
                        out.println("  Source:  " + m.getSourceRef());
                        // D-13: null glob -> "(all files)"
                        out.println("  Glob:    " + (m.getGlob() == null ? "(all files)" : m.getGlob()));
                        out.println("  Recurse: " + m.isRecurse());
                        out.println("  Sync:    " + m.isSync());
                    }
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
