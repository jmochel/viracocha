package org.saltations.infra;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for GlobMatcher. Plain JUnit 5, no @MicronautTest.
 * Covers MAP-05: glob pattern matching against paths.
 */
class GlobMatcherTest {

    @Test
    void plusInGlobPatternMatchesLiteralPlus() {
        assertTrue(GlobMatcher.matches("*+*.md", Path.of("file+name.md")));
    }

    @Test
    void plusGlobDoesNotMatchPathWithoutPlus() {
        assertFalse(GlobMatcher.matches("*+*.md", Path.of("filename.md")));
    }

    @Test
    void doubleStarMatchesAcrossDirectoryBoundaries() {
        assertTrue(GlobMatcher.matches("**/*.md", Path.of("docs/guide/readme.md")));
    }

    @Test
    void singleStarDoesNotCrossDirectoryBoundary() {
        assertFalse(GlobMatcher.matches("*.md", Path.of("docs/readme.md")));
    }

    @Test
    void simpleExtensionMatchWorks() {
        assertTrue(GlobMatcher.matches("*.java", Path.of("Main.java")));
    }

    @Test
    void extensionMismatchDoesNotMatch() {
        assertFalse(GlobMatcher.matches("*.java", Path.of("Main.txt")));
    }
}
