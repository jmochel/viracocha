package org.saltations.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.MappingEntry;
import org.saltations.model.ProjectEntry;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command: vira project add-mapping
 */
@Command(name = "add-mapping", description = "Add a pattern mapping to a project.", mixinStandardHelpOptions = true)
@Singleton
public class AddMappingCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--project"}, required = true, description = "Project name")
    private String project;

    @Option(names = {"--pattern"}, required = true, description = "Registered pattern name")
    private String pattern;

    @Option(names = {"--destination"}, required = true, description = "Destination path relative to project workspace")
    private String destination;

    @Option(names = {"--param"}, description = "Parameter as key=value (repeatable)")
    private List<String> paramPairs;

    private final ConfigService configService;

    @Inject
    public AddMappingCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            ViracochaConfig config = configService.load();

            ProjectEntry proj = config.getProjects().stream()
                .filter(e -> e.getName().equals(project))
                .findFirst()
                .orElse(null);
            if (proj == null) {
                spec.commandLine().getErr().println("Project '" + project + "' not found.");
                return 1;
            }

            boolean patternOk = config.getPatterns().stream().anyMatch(p -> p.getName().equals(pattern));
            if (!patternOk) {
                spec.commandLine().getErr().println("Pattern '" + pattern + "' not found.");
                return 1;
            }

            Map<String, String> params = parseParamPairs();
            if (params == null) {
                return 1;
            }

            String dest = destination.trim();
            proj.getMappings().add(new MappingEntry(pattern, dest, params));
            configService.save(config);
            spec.commandLine().getOut().println("Mapping added to project '" + project + "'.");
            return 0;
        } catch (ConfigNotInitializedException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 1;
        }
    }

    /**
     * @return parsed map, or null if invalid (error printed)
     */
    private Map<String, String> parseParamPairs() {
        Map<String, String> m = new LinkedHashMap<>();
        if (paramPairs == null || paramPairs.isEmpty()) {
            return m;
        }
        for (String pair : paramPairs) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                spec.commandLine().getErr().println("Error: --param must be key=value, got: " + pair);
                return null;
            }
            String key = pair.substring(0, eq).trim();
            String val = pair.substring(eq + 1).trim();
            m.put(key, val);
        }
        return m;
    }
}
