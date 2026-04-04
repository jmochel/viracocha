package org.saltations.sync;

import java.io.IOException;

/**
 * Contract for the filesystem sync engine (implementation loads config and walks subscriptions).
 */
public interface SyncService {

    /**
     * Runs sync for a project with optional subscription filter and execution mode.
     *
     * @param projectName project name in config
     * @param subscriptionIdOrNull when non-null, only the subscription with this id; when null, all subscriptions
     * @param dryRun when true, analyze only — no {@link java.nio.file.Files#copy} and no mutating creates
     * @param verbose when true, append human-readable lines to each result's {@link SyncSubscriptionResult#getVerboseLines()}
     */
    SyncEngineResult syncProject(String projectName, String subscriptionIdOrNull, boolean dryRun, boolean verbose)
        throws IOException;

    /**
     * Runs sync for every subscription on the named project (same as {@code syncProject(name, null, false, false)}).
     *
     * @param projectName project key as in configuration
     * @return aggregated per-subscription results
     * @throws IOException if configuration or filesystem access fails in a way that aborts the run
     */
    default SyncEngineResult syncProject(String projectName) throws IOException {
        return syncProject(projectName, null, false, false);
    }
}
