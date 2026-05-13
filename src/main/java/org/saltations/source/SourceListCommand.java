package org.saltations.source;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.model.SourceEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira source list
 * Lists all registered sources.
 * D-03: plain output — name + path, aligned columns, no header.
 * Exit codes: 0 = success (including empty list), 1 = config error / IO error.
 */
@Command(
    name = "list",
    aliases = {"ls"},
    description = "List all registered sources.",
    mixinStandardHelpOptions = true
)
@Singleton
public class SourceListCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    private final SourceService sourceService;

    @Inject
    public SourceListCommand(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @Override
    public Integer call() {
        try {
            var sources = sourceService.listSources();
            var out = spec.commandLine().getOut();
            // D-03: name + path, aligned columns, no header
            var maxNameWidth = sources.stream()
                .mapToInt(e -> e.getName().length())
                .max().orElse(0);
            for (SourceEntry e : sources) {
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
