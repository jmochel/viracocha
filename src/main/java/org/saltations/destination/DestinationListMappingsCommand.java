package org.saltations.destination;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Stub command: vira destination list-mappings
 * Full implementation provided in Plan 03.
 */
@Command(name = "list-mappings", description = "List mappings for a destination.", mixinStandardHelpOptions = true)
@Singleton
public class DestinationListMappingsCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }
}
