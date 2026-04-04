package org.saltations.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.ProjectEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira project list
 */
@Command(name = "list", description = "List all registered projects.", mixinStandardHelpOptions = true)
@Singleton
public class ListProjectsCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--json"}, description = "Output as JSON (one object per line)")
    private boolean json;

    private final ConfigService configService;

    @Inject
    public ListProjectsCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var config = configService.load();
            if (json) {
                ObjectMapper om = new ObjectMapper();
                for (ProjectEntry e : config.getProjects()) {
                    spec.commandLine().getOut().println(om.writeValueAsString(e));
                }
            } else {
                for (ProjectEntry e : config.getProjects()) {
                    spec.commandLine().getOut().printf("%-20s  %s%n", e.getName(), e.getPath());
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
}
