package org.saltations.sync;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigService;

import java.io.IOException;

/**
 * v3 sync engine — stub pending full implementation in Plan 02.
 */
@Singleton
public class DefaultSyncService implements SyncService {

    private final ConfigService configService;

    @Inject
    public DefaultSyncService(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public SyncResult sync(String destinationName, boolean dryRun, boolean verbose)
        throws IOException {
        throw new UnsupportedOperationException(
            "DefaultSyncService not yet implemented for v3 — see Phase 12 Plan 02.");
    }
}
