package org.saltations.destination;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Stub command: vira destination remove-mapping
 * Full implementation provided in Plan 03.
 */
@Command(name = "remove-mapping", description = "Remove a mapping from a destination.", mixinStandardHelpOptions = true)
@Singleton
public class DestinationRemoveMappingCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }
}
