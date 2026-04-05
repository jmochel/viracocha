package org.saltations.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira project unregister
 */
@Command(name = "unregister", description = "Remove a project from central config.", mixinStandardHelpOptions = true)
@Singleton
public class UnregisterProjectCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"-n", "--name"}, required = true, description = "Project name to remove")
    private String name;

    private final ConfigService configService;

    @Inject
    public UnregisterProjectCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var config = configService.load();
            boolean removed = config.getProjects().removeIf(e -> e.getName().equals(name));
            if (!removed) {
                spec.commandLine().getErr().println("Project '" + name + "' not found.");
                return 1;
            }
            configService.save(config);
            spec.commandLine().getOut().println("Project '" + name + "' unregistered.");
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
