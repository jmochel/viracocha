package org.saltations.generate;

import java.util.List;

/**
 * Outcome of a generate run: aggregate counts and optional per-file lines for {@code --verbose}.
 */
public record GenerationResult(
    int generated,
    int skipped,
    int failed,
    List<String> verboseLines
) {
    public static GenerationResult empty() {
        return new GenerationResult(0, 0, 0, List.of());
    }
}
