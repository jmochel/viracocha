package org.saltations.generate;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira generate
 */
@Command(name = "generate", description = "Generate workspace files from project mappings.", mixinStandardHelpOptions = true)
@Singleton
public class GenerateCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = {"--project-name"}, required = true, description = "Project name to generate")
    private String projectName;

    @Option(names = {"--dry-run"}, description = "Show actions without writing files")
    private boolean dryRun;

    @Option(names = {"--verbose"}, description = "Print one line per file action")
    private boolean verbose;

    private final GeneratorService generatorService;

    @Inject
    public GenerateCommand(GeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @Override
    public Integer call() {
        try {
            GenerationResult result = generatorService.generate(projectName, dryRun, verbose);
            if (verbose) {
                for (String line : result.verboseLines()) {
                    spec.commandLine().getOut().println(line);
                }
            }
            spec.commandLine().getOut().println(String.format(
                "Generated: %d files, Skipped: %d files, Failed: %d files",
                result.generated(), result.skipped(), result.failed()));
            return 0;
        } catch (ConfigNotInitializedException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IllegalArgumentException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 1;
        }
    }
}
