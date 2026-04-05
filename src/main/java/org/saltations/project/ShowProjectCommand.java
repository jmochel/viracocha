package org.saltations.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.MappingEntry;
import org.saltations.model.ProjectEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Command: vira project show
 */
@Command(name = "show", description = "Show details of a project.", mixinStandardHelpOptions = true)
@Singleton
public class ShowProjectCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"-n", "--name"}, required = true, description = "Project name")
    private String name;

    @Option(names = {"--json"}, description = "Output as JSON")
    private boolean json;

    private final ConfigService configService;

    @Inject
    public ShowProjectCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var config = configService.load();
            ProjectEntry entry = config.getProjects().stream()
                .filter(e -> e.getName().equals(name))
                .findFirst()
                .orElse(null);
            if (entry == null) {
                spec.commandLine().getErr().println("Project '" + name + "' not found.");
                return 1;
            }
            if (json) {
                spec.commandLine().getOut().println(new ObjectMapper().writeValueAsString(entry));
            } else {
                spec.commandLine().getOut().println("Name: " + entry.getName());
                spec.commandLine().getOut().println("Path: " + entry.getPath());
                if (entry.getMappings().isEmpty()) {
                    spec.commandLine().getOut().println("Mappings: (none)");
                } else {
                    int i = 1;
                    for (MappingEntry m : entry.getMappings()) {
                        spec.commandLine().getOut().println("Mapping " + i + ":");
                        spec.commandLine().getOut().println("  Pattern: " + m.getPatternName());
                        spec.commandLine().getOut().println("  Workspace: " + m.getWorkspacePath());
                        String params = formatParameters(m.getParameters());
                        spec.commandLine().getOut().println("  Parameters: " + params);
                        i++;
                    }
                }
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

    private static String formatParameters(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "(none)";
        }
        return parameters.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(", "));
    }
}
