package org.saltations.publisher;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Group command: vira publisher
 * Provides subcommands for managing registered publishers.
 */
@Command(
    name = "publisher",
    aliases = {"pub"},
    description = "Manage registered publishers.",
    mixinStandardHelpOptions = true,
    subcommands = {
        RegisterPublisherCommand.class,
        ListPublishersCommand.class,
        ShowPublisherCommand.class,
        UnregisterPublisherCommand.class
    }
)
@Singleton
public class PublisherCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }
}
