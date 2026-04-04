package org.saltations.sync;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outcome for a single subscription within one engine run.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncSubscriptionResult {

    private String subscriptionId;
    private int filesCopied;
    private int filesSkipped;
    private int filesFailed;
    private int conflicts;
    private List<SyncConflictRecord> conflictRecords = new ArrayList<>();
    /** Human-readable lines when {@code --verbose} (same role as {@link org.saltations.generate.GenerationResult#verboseLines()}). */
    private List<String> verboseLines = new ArrayList<>();
    /**
     * {@code true} when this subscription applied with no blocking conflicts
     * (planner: success when {@code conflicts == 0} after analyze).
     */
    private boolean success;
    /** Subscription-level failure without a path-specific conflict (e.g. unsupported direction). */
    private String errorMessage;
}
