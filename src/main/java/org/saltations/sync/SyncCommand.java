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
 * Command: vira sync — sync mapped files from sources to destinations.
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
    private String projectName;

    @Option(names = {"--mapping-id"}, description = "Limit sync to this mapping id")
    private String subscriptionId;

    @Option(names = {"--dry-run"}, description = "Analyze only; do not copy files or create directories")
    private boolean dryRun;

    @Option(names = {"--verbose"}, description = "Print one line per file action")
    private boolean verbose;

    @Option(names = {"--json"}, description = "Emit SyncEngineResult as JSON on stdout")
    private boolean json;

    private final SyncService syncService;

    @Inject
    public SyncCommand(SyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public Integer call() {
        if (projectName == null || projectName.isBlank()) {
            spec.commandLine().getErr().println("Missing required option: '--destination-name'");
            return 2;
        }
        String subId = (subscriptionId == null || subscriptionId.isBlank()) ? null : subscriptionId;
        try {
            SyncEngineResult result = syncService.syncProject(projectName, subId, dryRun, verbose);
            if (json) {
                ObjectMapper om = new ObjectMapper();
                spec.commandLine().getOut().println(om.writeValueAsString(result));
                return exitCode(result);
            }
            if (verbose) {
                for (SyncSubscriptionResult sub : result.getSubscriptionResults()) {
                    for (String line : sub.getVerboseLines()) {
                        spec.commandLine().getOut().println(line);
                    }
                }
            }
            int copied = 0;
            int skipped = 0;
            int failed = 0;
            int conflicts = 0;
            for (SyncSubscriptionResult sub : result.getSubscriptionResults()) {
                copied += sub.getFilesCopied();
                skipped += sub.getFilesSkipped();
                failed += sub.getFilesFailed();
                conflicts += sub.getConflicts();
            }
            spec.commandLine().getOut().println(String.format(
                "Copied: %d, Skipped: %d, Failed: %d, Conflicts: %d",
                copied, skipped, failed, conflicts));
            for (SyncSubscriptionResult sub : result.getSubscriptionResults()) {
                for (SyncConflictRecord rec : sub.getConflictRecords()) {
                    spec.commandLine().getErr().println(
                        "CONFLICT %s %s".formatted(rec.getRelativePath(), rec.getKind()));
                }
            }
            return exitCode(result);
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

    private static int exitCode(SyncEngineResult result) {
        for (SyncSubscriptionResult sub : result.getSubscriptionResults()) {
            if (sub.getConflicts() > 0 || sub.getFilesFailed() > 0) {
                return 1;
            }
        }
        return 0;
    }
}
