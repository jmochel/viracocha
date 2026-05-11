package org.saltations.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One path-level conflict within a sync run (v3: per-field removed per D-08).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncConflictRecord {

    /** POSIX-style relative path string for reporting. */
    private String relativePath;
    private SyncConflictKind kind;
    /** Optional detail for logs or verbose CLI output. */
    private String message;
}
