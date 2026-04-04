package org.saltations.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One path-level conflict or blocked outcome within a subscription sync.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncConflictRecord {

    private String subscriptionId;
    /** POSIX-style relative path string for reporting. */
    private String relativePath;
    private SyncConflictKind kind;
    /** Optional detail for logs or verbose CLI output. */
    private String message;
}
