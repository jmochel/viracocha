package org.saltations.source;

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
 * Command: vira source remove NAME
 * Removes a named source from the config.
 * D-16: exits 1 with "Source 'NAME' not found." if name is not registered.
 * Exit codes: 0 = success, 1 = not found / config error / IO error.
 */
@Command(
    name = "remove",
    aliases = {"rm"},
    description = "Remove a named source from the configuration.",
    mixinStandardHelpOptions = true
)
@Singleton
public class SourceRemoveCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", description = "Name of the source to remove.")
    private String name;

    private final SourceService sourceService;

    @Inject
    public SourceRemoveCommand(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @Override
    public Integer call() {
        try {
            var removed = sourceService.removeSource(name);
            if (!removed) {
                spec.commandLine().getErr().println("Source '" + name + "' not found.");
                return 1;
            }
            spec.commandLine().getOut().println("Source '" + name + "' removed.");
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
