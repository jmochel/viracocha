package org.saltations.sync;

import java.io.IOException;

/**
 * Contract for the v3 filesystem sync engine.
 */
public interface SyncService {

    /**
     * Runs sync for a named destination, applying all mappings with {@code sync: true}.
     *
     * @param destinationName destination name in config
     * @param dryRun          when true, analyze only — no filesystem writes
     * @param verbose         when true, populate verboseLines in the result with per-file details
     * @return SyncResult with counts and optional verbose lines
     * @throws IOException if config or filesystem access fails
     */
    SyncResult sync(String destinationName, boolean dryRun, boolean verbose) throws IOException;
}
