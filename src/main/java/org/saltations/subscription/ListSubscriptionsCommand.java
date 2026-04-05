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
 * Command: vira subscription list
 */
@Command(name = "list", description = "List subscriptions (optionally filtered by project).", mixinStandardHelpOptions = true)
@Singleton
public class ListSubscriptionsCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--project"}, description = "Filter to subscriptions for this project only")
    private String projectFilter;

    @Option(names = {"--json"}, description = "JSONL output")
    private boolean json;

    private final ConfigService configService;

    @Inject
    public ListSubscriptionsCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            ViracochaConfig config = configService.load();
            ObjectMapper om = new ObjectMapper();
            for (ProjectEntry proj : config.getProjects()) {
                if (projectFilter != null && !projectFilter.equals(proj.getName())) {
                    continue;
                }
                for (SubscriptionEntry sub : proj.getSubscriptions()) {
                    if (json) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", sub.getId());
                        row.put("project", proj.getName());
                        row.put("catalog", sub.getCatalogName());
                        row.put("source", sub.getSourcePath());
                        row.put("workspacePath", sub.getWorkspacePath());
                        row.put("direction", sub.getDirection().name());
                        spec.commandLine().getOut().println(om.writeValueAsString(row));
                    } else {
                        String idCol = truncate(sub.getId(), 36);
                        String workspacePathCol = truncate(sub.getWorkspacePath(), 48);
                        spec.commandLine().getOut().printf(
                            "%-36s  %-15s  %-20s  %-24s  %s%n",
                            idCol,
                            proj.getName(),
                            sub.getCatalogName(),
                            sub.getDirection().name(),
                            workspacePathCol);
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

    static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        if (max <= 3) {
            return s.substring(0, max);
        }
        return s.substring(0, max - 3) + "...";
    }
}
