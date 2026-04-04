package org.saltations.sync;

/**
 * Classifies a sync conflict or blocked path for structured reporting (Phase 7 JSON / CLI).
 */
public enum SyncConflictKind {

    /**
     * Both sides exist as regular files but byte content differs ({@code Files.mismatch}).
     */
    CONTENT_MISMATCH,

    /**
     * One side is a regular file and the other is a directory at the same relative path.
     */
    TYPE_MISMATCH,

    /**
     * A symbolic link was present where sync does not follow links (explicit blocked outcome).
     */
    SYMLINK_UNSUPPORTED;
}
