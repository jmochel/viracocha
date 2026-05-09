package org.saltations.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.model.SourceEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command: vira source list [--json]
 * Lists all registered sources.
 * D-03: plain output — name + path, aligned columns, no header.
 * D-04: --json switches to JSONL (one JSON object per line).
 * Exit codes: 0 = success (including empty list), 1 = config error / IO error.
 */
@Command(
    name = "list",
    description = "List all registered sources.",
    mixinStandardHelpOptions = true
)
@Singleton
public class SourceListCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = {"--json"},
            description = "Output as JSONL (one JSON object per line).")
    private boolean json;

    private final SourceService sourceService;

    @Inject
    public SourceListCommand(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @Override
    public Integer call() {
        try {
            List<SourceEntry> sources = sourceService.listSources();
            PrintWriter out = spec.commandLine().getOut();
            if (json) {
                ObjectMapper om = new ObjectMapper();
                for (SourceEntry e : sources) {
                    out.println(om.writeValueAsString(e));
                }
            } else {
                // D-03: name + path, aligned columns, no header
                int maxNameWidth = sources.stream()
                    .mapToInt(e -> e.getName().length())
                    .max().orElse(0);
                for (SourceEntry e : sources) {
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
