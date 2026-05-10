package org.saltations.destination;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Stub command: vira destination add-mapping
 * Full implementation provided in Plan 03.
 */
@Command(name = "add-mapping", description = "Add a mapping to a destination.", mixinStandardHelpOptions = true)
@Singleton
public class DestinationAddMappingCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }
}
