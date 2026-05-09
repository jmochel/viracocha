package org.saltations.source;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Group command: vira source (alias: vira src)
 * Routes to subcommands for managing registered local directory sources.
 * D-10: subcommands = {SourceAddCommand, SourceListCommand, SourceShowCommand, SourceRemoveCommand}
 */
@Command(
    name = "source",
    aliases = {"src"},
    description = "Manage registered local directory sources.",
    mixinStandardHelpOptions = true,
    subcommands = {
        SourceAddCommand.class,
        SourceListCommand.class,
        SourceShowCommand.class,
        SourceRemoveCommand.class
    }
)
@Singleton
public class SourceCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }
}
