package org.saltations.generate;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigService;
import org.saltations.infra.GlobMatcher;
import org.saltations.infra.HiddenPathFilter;
import org.saltations.model.DestinationEntry;
import org.saltations.model.MappingEntry;
import org.saltations.model.SourceEntry;
import org.saltations.model.ViracochaConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Core v3 traversal engine. Wires together ConfigService, GlobMatcher, HiddenPathFilter,
 * and PathExpander to implement the generate algorithm (GEN-01 through GEN-04).
 */
@Singleton
public class GeneratorService {

    private final ConfigService configService;
    private final PathExpander pathExpander;

    @Inject
    public GeneratorService(ConfigService configService, PathExpander pathExpander) {
        this.configService = configService;
        this.pathExpander = pathExpander;
    }

    /**
     * Generates destination workspace from registered sources via mappings.
     *
     * <ul>
     *   <li>GEN-01: Files from all mapped sources are written to the destination path.</li>
     *   <li>GEN-02: Re-running skips already-present files and counts them as skipped.</li>
     *   <li>GEN-03: Template sources expand Freemarker variables in path segments and file content.</li>
     *   <li>GEN-04: Binary (non-template) sources are byte-copied using Files.copy() without string read.</li>
     * </ul>
     *
     * @param destinationName name of the registered destination to generate
     * @param dryRun          when true, compute what would be generated but do not write files
     * @param verbose         when true, populate verboseLines in the result with per-file details
     * @return GenerationResult with counts of generated, skipped, and failed files
     * @throws IllegalArgumentException if destinationName is not found in config
     * @throws IOException              if config cannot be read or files cannot be written
     */
    public GenerationResult generate(String destinationName, boolean dryRun, boolean verbose) throws IOException {
        // STEP 1 — Load config
        ViracochaConfig config = configService.load();

        // STEP 2 — Find destination (Pitfall 6 guard)
        DestinationEntry dest = config.getDestinations().stream()
            .filter(d -> d.getName().equals(destinationName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Destination '" + destinationName + "' not found."));

        // STEP 3 — Tilde expansion (D-10)
        String rawPath = dest.getPath();
        String resolvedPath = rawPath.startsWith("~")
            ? System.getProperty("user.home") + rawPath.substring(1)
            : rawPath;
        Path destRoot = Path.of(resolvedPath);

        // STEP 4 — Missing destination directory check (D-05 through D-09)
        // Auto-create silently so service tests work without stdin interaction.
        // Full interactive prompt is handled in Plan 02 (GenerateCommand integration).
        if (!Files.exists(destRoot)) {
            if (!dryRun) {
                Files.createDirectories(destRoot);
            }
        }

        // STEP 5 — Accumulators
        int generated = 0;
        int skipped = 0;
        int failed = 0;
        List<String> verboseLines = new ArrayList<>();

        // STEP 6 — For each mapping, traverse source
        for (MappingEntry mapping : dest.getMappings()) {
            // Resolve source
            SourceEntry source = config.getSources().stream()
                .filter(s -> s.getName().equals(mapping.getSourceRef()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Source '" + mapping.getSourceRef() + "' not found."));

            Path sourceRoot = Path.of(source.getPath());
            // Pitfall 1 note: filter(Files::isRegularFile) removes root dir and subdirs from stream
            int maxDepth = mapping.isRecurse() ? Integer.MAX_VALUE : 1;

            List<Path> files;
            try (Stream<Path> stream = Files.walk(sourceRoot, maxDepth)) {
                files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !HiddenPathFilter.hasHiddenPathSegment(sourceRoot, p))
                    .filter(p -> {
                        String glob = mapping.getGlob();
                        if (glob == null) return true;
                        // Pitfall 2: always relativize before passing to GlobMatcher
                        Path rel = sourceRoot.relativize(p);
                        return GlobMatcher.matches(glob, rel);
                    })
                    .sorted()
                    .collect(Collectors.toList());
            }

            for (Path sourcePath : files) {
                // Compute dest path with optional template segment expansion (D-12)
                Path relPath = sourceRoot.relativize(sourcePath);
                Path destPath = destRoot;
                Map<String, String> params = dest.getParameters();
                boolean fileFailedDuringPathExpansion = false;

                for (int i = 0; i < relPath.getNameCount(); i++) {
                    String seg = relPath.getName(i).toString();
                    String expanded;
                    if (source.isTemplates()) {
                        // Pitfall 3: catch IllegalArgumentException from expandSegment, increment failed, skip file
                        try {
                            expanded = pathExpander.expandSegment(seg, params);
                        } catch (IllegalArgumentException e) {
                            failed++;
                            if (verbose) verboseLines.add("Failed " + sourcePath);
                            fileFailedDuringPathExpansion = true;
                            break;
                        }
                    } else {
                        expanded = seg;
                    }
                    destPath = destPath.resolve(expanded);
                }

                if (fileFailedDuringPathExpansion) {
                    continue; // skip this file
                }

                // Skip existing (GEN-02)
                if (Files.exists(destPath)) {
                    skipped++;
                    if (verbose) verboseLines.add("Skipped " + destPath);
                    continue;
                }

                // Write or dry-run
                if (dryRun) {
                    generated++;  // count as would-generate
                    if (verbose) verboseLines.add("Would create " + destPath);
                } else {
                    try {
                        // D-09: auto-create subdirs silently
                        Files.createDirectories(destPath.getParent());
                        if (source.isTemplates()) {
                            // GEN-03, D-12: Template — expand file content
                            String rawContent = Files.readString(sourcePath, StandardCharsets.UTF_8);
                            String expandedContent = pathExpander.expandSegment(rawContent, params);
                            Files.writeString(destPath, expandedContent, StandardCharsets.UTF_8);
                        } else {
                            // GEN-04, D-11: Binary byte-copy — no REPLACE_EXISTING; exists check already done
                            Files.copy(sourcePath, destPath);
                        }
                        generated++;
                        if (verbose) verboseLines.add("Created " + destPath);
                    } catch (IllegalArgumentException | IOException e) {
                        failed++;
                        if (verbose) verboseLines.add("Failed " + destPath);
                    }
                }
            }
        }

        return new GenerationResult(generated, skipped, failed, verboseLines);
    }
}
