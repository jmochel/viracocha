package org.saltations.subscription;

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
import java.util.concurrent.Callable;

/**
 * Command: vira subscription remove
 */
@Command(name = "remove", description = "Remove a subscription by id.", mixinStandardHelpOptions = true)
@Singleton
public class RemoveSubscriptionCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--id"}, required = true, description = "Subscription id to remove")
    private String id;

    private final ConfigService configService;

    @Inject
    public RemoveSubscriptionCommand(ConfigService configService) {
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
            owner.getSubscriptions().remove(found);
            configService.save(config);
            spec.commandLine().getOut().println("Subscription removed.");
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
