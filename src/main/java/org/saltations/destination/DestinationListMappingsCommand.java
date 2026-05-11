package org.saltations.destination;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.model.MappingEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command: vira destination list-mappings NAME [--json]
 * Lists all mappings for the named destination.
 * D-19: numbered mapping blocks (Mapping N: / Source: / Glob: / Recurse: / Sync:).
 * --json: JSONL (one JSON object per line).
 * No mappings: "No mappings for destination '<name>'." exit 0.
 */
@Command(
    name = "list-mappings",
    description = "List all mappings for a destination.",
    mixinStandardHelpOptions = true
)
@Singleton
public class DestinationListMappingsCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", description = "Name of the destination.")
    private String destName;

    @Option(names = {"--json"},
            description = "Output as JSONL (one JSON object per line).")
    private boolean json;

    private final DestinationService destinationService;

    @Inject
    public DestinationListMappingsCommand(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @Override
    public Integer call() {
        try {
            List<MappingEntry> mappings = destinationService.listMappings(destName);
            PrintWriter out = spec.commandLine().getOut();
            if (mappings.isEmpty()) {
                out.println("No mappings for destination '" + destName + "'.");
                out.flush();
                return 0;
            }
            if (json) {
                ObjectMapper om = new ObjectMapper();
                for (MappingEntry m : mappings) {
                    out.println(om.writeValueAsString(m));
                }
            } else {
                for (int i = 0; i < mappings.size(); i++) {
                    MappingEntry m = mappings.get(i);
                    out.println("Mapping " + (i + 1) + ":");
                    out.println("  Source:  " + m.getSourceRef());
                    out.println("  Glob:    " + (m.getGlob() == null ? "(all files)" : m.getGlob()));
                    out.println("  Recurse: " + m.isRecurse());
                    out.println("  Sync:    " + m.isSync());
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
