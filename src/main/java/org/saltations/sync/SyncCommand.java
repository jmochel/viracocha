package org.saltations.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira sync — sync mapped files from sources to destinations (v3).
 * Per-mapping filter removed (D-10). --destination-name required (D-11).
 */
@Command(
    name = "sync",
    description = "Sync files from source directories to destination workspaces per mapping rules.",
    mixinStandardHelpOptions = true)
@Singleton
public class SyncCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = {"--destination-name"}, description = "Destination name in configuration")
    private String destinationName;

    @Option(names = {"--dry-run"}, description = "Analyze only; do not copy files")
    private boolean dryRun;

    @Option(names = {"--verbose"}, description = "Print one line per file action")
    private boolean verbose;

    @Option(names = {"--json"}, description = "Emit SyncResult as JSON on stdout")
    private boolean json;

    private final SyncService syncService;

    @Inject
    public SyncCommand(SyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public Integer call() {
        if (destinationName == null || destinationName.isBlank()) {
            spec.commandLine().getErr().println("Missing required option: '--destination-name'");
            return 2;
        }
        try {
            SyncResult result = syncService.sync(destinationName, dryRun, verbose);
            if (json) {
                ObjectMapper om = new ObjectMapper();
                spec.commandLine().getOut().println(om.writeValueAsString(result));
                return result.conflicts() > 0 || result.failed() > 0 ? 1 : 0;
            }
            if (verbose) {
                for (String line : result.verboseLines()) {
                    spec.commandLine().getOut().println(line);
                }
            }
            spec.commandLine().getOut().println(String.format(
                "Copied: %d, Skipped: %d, Failed: %d, Conflicts: %d",
                result.copied(), result.skipped(), result.failed(), result.conflicts()));
            for (SyncConflictRecord rec : result.conflictRecords()) {
                spec.commandLine().getErr().println(
                    "CONFLICT %s %s".formatted(rec.getRelativePath(), rec.getKind()));
            }
            return result.conflicts() > 0 || result.failed() > 0 ? 1 : 0;
        } catch (ConfigNotInitializedException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IllegalArgumentException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 1;
        }
    }
}
