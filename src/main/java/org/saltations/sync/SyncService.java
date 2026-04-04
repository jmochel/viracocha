package org.saltations.sync;

import java.io.IOException;

/**
 * Contract for the filesystem sync engine (implementation loads config and walks subscriptions).
 */
public interface SyncService {

    /**
     * Runs sync for every subscription on the named project.
     * <p>
     * The implementation loads configuration (typically via {@code ConfigService}), resolves
     * publishers and workspaces, and processes <strong>all</strong> subscriptions for that project.
     * Phase 7 may add filtering (e.g. by subscription id).
     * </p>
     * <p>
     * Per {@code 06-CONTEXT.md} D-12: processing is best-effort <strong>per subscription</strong> —
     * each subscription yields a {@link SyncSubscriptionResult}; one subscription's conflicts do not
     * necessarily stop others from being analyzed; the aggregate {@link SyncEngineResult} lists each outcome.
     * </p>
     *
     * @param projectName project key as in configuration
     * @return aggregated per-subscription results
     * @throws IOException if configuration or filesystem access fails in a way that aborts the run
     */
    SyncEngineResult syncProject(String projectName) throws IOException;
}
