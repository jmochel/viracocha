package org.saltations.subscription;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.CatalogEntry;
import org.saltations.model.ProjectEntry;
import org.saltations.model.SubscriptionEntry;
import org.saltations.model.SubscriptionSyncDirection;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Command: vira subscription add
 */
@Command(
    name = "add",
    description = "Add a subscription linking a catalog path into a project workspace path.",
    mixinStandardHelpOptions = true)
@Singleton
public class AddSubscriptionCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--project"}, required = true, description = "Project name")
    private String project;

    @Option(names = {"--catalog"}, required = true, description = "Registered catalog name")
    private String catalog;

    @Option(names = {"--source"}, required = true, description = "Path relative to catalog root")
    private String source;

    @Option(names = {"-w", "--workspace"}, required = true, description = "Path relative to project workspace")
    private String workspace;

    @Option(names = {"--direction"}, required = true, description = "Sync direction (kebab-case)")
    private String directionRaw;

    private final ConfigService configService;

    @Inject
    public AddSubscriptionCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        SubscriptionSyncDirection direction;
        try {
            direction = SubscriptionSyncDirection.fromCliKebab(directionRaw.trim());
        } catch (IllegalArgumentException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        }

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

            CatalogEntry cat = config.getCatalogs().stream()
                .filter(e -> e.getName().equals(catalog))
                .findFirst()
                .orElse(null);
            if (cat == null) {
                spec.commandLine().getErr().println("Catalog '" + catalog + "' not found.");
                return 1;
            }

            String src = source.trim();
            String workspacePath = workspace.trim();
            if (isUnsafeRelative(src) || isUnsafeRelative(workspacePath)) {
                spec.commandLine().getErr().println("Error: invalid path: " + (isUnsafeRelative(src) ? src : workspacePath));
                return 1;
            }

            for (SubscriptionEntry existing : proj.getSubscriptions()) {
                if (existing.getCatalogName().equals(catalog)
                    && existing.getSourcePath().equals(src)
                    && existing.getWorkspacePath().equals(workspacePath)) {
                    spec.commandLine().getErr().println(
                        "Subscription already exists for this project with the same catalog, source, and workspace path.");
                    return 1;
                }
            }

            String catalogPath = cat.getPath();
            if (!Files.exists(Path.of(catalogPath))) {
                spec.commandLine().getErr().println("Warning: catalog path does not exist on disk: " + catalogPath);
            }

            String newId = UUID.randomUUID().toString();
            while (idExistsGlobally(config, newId)) {
                newId = UUID.randomUUID().toString();
            }

            SubscriptionEntry entry = new SubscriptionEntry(newId, catalog, src, workspacePath, direction);
            proj.getSubscriptions().add(entry);
            configService.save(config);
            spec.commandLine().getOut().println("Subscription added (id=" + newId + ").");
            return 0;
        } catch (ConfigNotInitializedException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 1;
        }
    }

    private static boolean idExistsGlobally(ViracochaConfig config, String id) {
        for (ProjectEntry p : config.getProjects()) {
            for (SubscriptionEntry s : p.getSubscriptions()) {
                if (id.equals(s.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Rejects absolute paths, Windows drive roots, and {@code ..} segments.
     */
    boolean isUnsafeRelative(String p) {
        if (p == null) {
            return true;
        }
        String t = p.trim();
        if (t.isEmpty()) {
            return true;
        }
        if (t.startsWith("/")) {
            return true;
        }
        if (t.length() >= 2 && Character.isLetter(t.charAt(0)) && t.charAt(1) == ':') {
            return true;
        }
        for (String seg : t.split("[/\\\\]+")) {
            if (seg.isEmpty()) {
                continue;
            }
            if ("..".equals(seg)) {
                return true;
            }
        }
        return false;
    }
}
