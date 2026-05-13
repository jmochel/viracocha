package org.saltations.infra;

import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Utility for matching paths against glob patterns.
 * Uses JDK {@link FileSystems#getDefault()} PathMatcher with the "glob:" syntax.
 * MAP-05: callers pass patterns WITHOUT the "glob:" prefix (e.g. "**&#47;*.md").
 */
public final class GlobMatcher {

    private GlobMatcher() {
    }

    /**
     * Returns true if {@code path} matches the given {@code glob} pattern.
     *
     * @param glob glob pattern WITHOUT "glob:" prefix (e.g. "**&#47;*.md", "*.java", "*+*.md")
     * @param path path to test
     * @return true if the path matches the pattern
     */
    public static boolean matches(String glob, Path path) {
        var matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        return matcher.matches(path);
    }
}
