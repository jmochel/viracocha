package org.saltations.pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.PatternEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira pattern show
 * Per D-02: Name: / Path: / Parameters: (multi-line key-value block)
 * Per D-03: --json outputs a single JSON object (with parameters array)
 * Exit codes: 0 = success, 1 = not found or any error
 */
@Command(name = "show", description = "Show details of a registered pattern.", mixinStandardHelpOptions = true)
@Singleton
public class ShowPatternCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--name"}, required = true, description = "Pattern name")
    private String name;

    @Option(names = {"--json"}, description = "Output as JSON")
    private boolean json;

    private final ConfigService configService;

    @Inject
    public ShowPatternCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var config = configService.load();
            PatternEntry entry = config.getPatterns().stream()
                .filter(e -> e.getName().equals(name))
                .findFirst()
                .orElse(null);
            if (entry == null) {
                spec.commandLine().getErr().println("Pattern '" + name + "' not found.");
                return 1;
            }
            if (json) {
                spec.commandLine().getOut().println(new ObjectMapper().writeValueAsString(entry));
            } else {
                spec.commandLine().getOut().println("Name: " + entry.getName());
                spec.commandLine().getOut().println("Path: " + entry.getPath());
                String params = entry.getParameters().isEmpty()
                    ? "(none)"
                    : String.join(", ", entry.getParameters());
                spec.commandLine().getOut().println("Parameters: " + params);
            }
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
