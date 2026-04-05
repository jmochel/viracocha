package org.saltations.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.ProjectEntry;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

/**
 * Command: vira project create
 */
@Command(name = "create", description = "Register a named project with a workspace path.", mixinStandardHelpOptions = true)
@Singleton
public class CreateProjectCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"-n", "--name"}, required = true, description = "Project name")
    private String name;

    @Option(names = {"-p", "--path"}, required = true, description = "Absolute path to project workspace root")
    private String path;

    private final ConfigService configService;

    @Inject
    public CreateProjectCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            ViracochaConfig config = configService.load();

            if (!Files.exists(Path.of(path))) {
                spec.commandLine().getErr().println("Error: path does not exist: " + path);
                return 1;
            }

            boolean exists = config.getProjects().stream().anyMatch(e -> e.getName().equals(name));
            if (exists) {
                spec.commandLine().getErr().println("Project '" + name + "' already exists.");
                return 1;
            }

            config.getProjects().add(new ProjectEntry(name, path, new ArrayList<>(), new LinkedHashMap<>(), new ArrayList<>()));
            configService.save(config);
            spec.commandLine().getOut().println("Project '" + name + "' created.");
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
