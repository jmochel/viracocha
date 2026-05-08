package org.saltations.infra;

import java.nio.file.Path;

/**
 * Shared path rules for directory walks. Replaces ArchetypePathUtils.
 */
public final class HiddenPathFilter {

    private HiddenPathFilter() {
    }

    /**
     * True if any path segment under {@code root} (including {@code path}'s filename) starts with ".".
     */
    public static boolean hasHiddenPathSegment(Path root, Path path) {
        Path rel = root.relativize(path);
        for (int i = 0; i < rel.getNameCount(); i++) {
            if (rel.getName(i).toString().startsWith(".")) {
                return true;
            }
        }
        return false;
    }
}
