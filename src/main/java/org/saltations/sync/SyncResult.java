package org.saltations.sync;

import java.util.List;

/**
 * Outcome of a sync run: aggregate counts and optional per-file lines for {@code --verbose}.
 * Replaces v2 SyncEngineResult and SyncSubscriptionResult (D-07).
 */
public record SyncResult(
    int copied,
    int skipped,
    int failed,
    int conflicts,
    List<String> verboseLines,
    List<SyncConflictRecord> conflictRecords
) {
    public static SyncResult empty() {
        return new SyncResult(0, 0, 0, 0, List.of(), List.of());
    }
}
