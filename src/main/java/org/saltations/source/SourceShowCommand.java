package org.saltations.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.model.SourceEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Command: vira source show NAME [--json]
 * Displays full details for a named source.
 * D-05: multi-line key-value output (Name, Path, Templates, Parameters).
 * D-06: Parameters block omitted if templates=false or parameter list is empty.
 * D-07: --json outputs a single JSON object.
 * Exit codes: 0 = success, 1 = not found / config error / IO error.
 */
@Command(
    name = "show",
    description = "Display full details for a named source.",
    mixinStandardHelpOptions = true
)
@Singleton
public class SourceShowCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", description = "Name of the source to display.")
    private String name;

    @Option(names = {"--json"},
            description = "Output as a single JSON object.")
    private boolean json;

    private final SourceService sourceService;

    @Inject
    public SourceShowCommand(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @Override
    public Integer call() {
        try {
            Optional<SourceEntry> result = sourceService.getSource(name);
            if (result.isEmpty()) {
                spec.commandLine().getErr().println("Source '" + name + "' not found.");
                return 1;
            }
            SourceEntry entry = result.get();
            PrintWriter out = spec.commandLine().getOut();
            if (json) {
                // D-07: single JSON object
                ObjectMapper om = new ObjectMapper();
                out.println(om.writeValueAsString(entry));
            } else {
                // D-05: multi-line key-value block (exact order)
                out.println("Name:      " + entry.getName());
                out.println("Path:      " + entry.getPath());
                out.println("Templates: " + entry.isTemplates());
                // D-06: Parameters block only when templates=true AND parameters non-empty
                if (entry.isTemplates() && !entry.getParameters().isEmpty()) {
                    out.println("Parameters:");
                    for (String param : entry.getParameters()) {
                        out.println("  " + param);
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
