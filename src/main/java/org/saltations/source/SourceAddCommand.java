package org.saltations.source;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

/**
 * Command: vira source add --name NAME --path PATH [--templates]
 * Registers a named local directory as a source.
 * Exit codes: 0 = success, 1 = validation failure / config error / IO error.
 * D-11: output via spec.commandLine().getOut() / .getErr() only.
 */
@Command(
    name = "add",
    description = "Register a named local directory as a source.",
    mixinStandardHelpOptions = true
)
@Singleton
public class SourceAddCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = {"-n", "--name"}, required = true,
            description = "Unique name for this source.")
    private String name;

    @Option(names = {"-p", "--path"}, required = true,
            description = "Absolute or relative path to the source directory.")
    private String path;

    @Option(names = {"--has-templates","-ht"},
            description = "Extract Freemarker variable names from template files in this source.")
    private boolean templates;

    private final SourceService sourceService;

    @Inject
    public SourceAddCommand(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @Override
    public Integer call() {
        try {
            var entry = sourceService.addSource(name, path, templates);
            spec.commandLine().getOut().println("Source '" + entry.getName() + "' added.");
            return 0;
        } catch (Exception e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        }
   
    }
}
