package org.saltations.pattern;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Group command: vira pattern
 * Provides subcommands for managing registered patterns.
 */
@Command(
    name = "pattern",
    description = "Manage registered patterns.",
    mixinStandardHelpOptions = true,
    subcommands = {
        RegisterPatternCommand.class,
        ListPatternsCommand.class,
        ShowPatternCommand.class,
        UnregisterPatternCommand.class
    }
)
@Singleton
public class PatternCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }
}
