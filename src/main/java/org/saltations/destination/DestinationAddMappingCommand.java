package org.saltations.destination;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * vira destination add-mapping DEST-NAME --source SOURCE-NAME [--glob PATTERN] [--recurse] [--sync]
 * D-16: success message "Mapping added to destination '<name>'."
 * D-17: dest not found prints "Destination '<name>' not found." exit 1
 * D-18: source not found prints "Source '<sourceRef>' not found." exit 1
 */
@Command(
    name = "map-add",
    description = "Add a source mapping to a destination.",
    mixinStandardHelpOptions = true
)
@Singleton
public class DestinationAddMappingCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(index = "0", description = "Name of the destination.")
    private String destName;

    @Option(names = {"--source","-src"}, required = true,
            description = "Name of the source to map from.")
    private String sourceRef;

    @Option(names = {"--glob","-g"},
            description = "Glob pattern filter (null = copy all files).")
    private String glob;

    @Option(names = {"--recurse","-r"},
            description = "Walk source directory recursively.")
    private boolean recurse;

    @Option(names = {"--syncable","-syn"},
            description = "Keep destination in sync on 'vira sync'.")
    private boolean sync;

    private final DestinationService destinationService;

    @Inject
    public DestinationAddMappingCommand(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @Override
    public Integer call() {
        try {
            destinationService.addMapping(destName, sourceRef, glob, recurse, sync);
            spec.commandLine().getOut().println("Mapping added to destination '" + destName + "'.");
            return 0;
        } catch (ConfigNotInitializedException | IllegalArgumentException | IOException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        }
    }
}
