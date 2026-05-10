package org.saltations.destination;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Group command: vira destination (alias: vira dest)
 * Routes to subcommands for managing registered destination workspaces.
 * D-02: aliases = {"dest"} (locked decision).
 * 7 subcommands: 4 CRUD (Plan 02) + 3 mapping commands (Plan 03 — stubs here).
 */
@Command(
    name = "destination",
    aliases = {"dest"},
    description = "Manage registered destination workspaces.",
    mixinStandardHelpOptions = true,
    subcommands = {
        DestinationAddCommand.class,
        DestinationListCommand.class,
        DestinationShowCommand.class,
        DestinationRemoveCommand.class,
        DestinationAddMappingCommand.class,
        DestinationListMappingsCommand.class,
        DestinationRemoveMappingCommand.class
    }
)
@Singleton
public class DestinationCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }
}
