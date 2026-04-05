package org.saltations.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.ProjectEntry;
import org.saltations.model.SubscriptionEntry;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command: vira subscription show
 */
@Command(name = "show", description = "Show one subscription by id.", mixinStandardHelpOptions = true)
@Singleton
public class ShowSubscriptionCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--id"}, required = true, description = "Subscription id")
    private String id;

    @Option(names = {"--json"}, description = "JSON object output")
    private boolean json;

    private final ConfigService configService;

    @Inject
    public ShowSubscriptionCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            ViracochaConfig config = configService.load();
            ProjectEntry owner = null;
            SubscriptionEntry found = null;
            for (ProjectEntry proj : config.getProjects()) {
                for (SubscriptionEntry sub : proj.getSubscriptions()) {
                    if (id.equals(sub.getId())) {
                        owner = proj;
                        found = sub;
                        break;
                    }
                }
                if (found != null) {
                    break;
                }
            }
            if (found == null || owner == null) {
                spec.commandLine().getErr().println("Subscription '" + id + "' not found.");
                return 1;
            }
            if (json) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", found.getId());
                row.put("project", owner.getName());
                row.put("catalog", found.getCatalogName());
                row.put("source", found.getSourcePath());
                row.put("workspacePath", found.getWorkspacePath());
                row.put("direction", found.getDirection().name());
                spec.commandLine().getOut().println(new ObjectMapper().writeValueAsString(row));
            } else {
                spec.commandLine().getOut().println("id: " + found.getId());
                spec.commandLine().getOut().println("project: " + owner.getName());
                spec.commandLine().getOut().println("catalog: " + found.getCatalogName());
                spec.commandLine().getOut().println("source: " + found.getSourcePath());
                spec.commandLine().getOut().println("workspacePath: " + found.getWorkspacePath());
                spec.commandLine().getOut().println("direction: " + found.getDirection().name());
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
