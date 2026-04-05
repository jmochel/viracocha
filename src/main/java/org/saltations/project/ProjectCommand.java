package org.saltations.project;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Group command: vira project
 */
@Command(
    name = "project",
    aliases = {"proj"},
    description = "Manage projects and mappings.",
    mixinStandardHelpOptions = true,
    subcommands = {
        CreateProjectCommand.class,
        ListProjectsCommand.class,
        AddMappingCommand.class,
        ShowProjectCommand.class,
        UnregisterProjectCommand.class
    }
)
@Singleton
public class ProjectCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }
}
