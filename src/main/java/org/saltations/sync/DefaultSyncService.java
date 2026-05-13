package org.saltations.sync;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigService;
import org.saltations.infra.GlobMatcher;
import org.saltations.infra.HiddenPathFilter;
import org.saltations.model.MappingEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * v3 sync engine — copies changed source files to the destination using timestamp-based
 * conflict detection. Template sources are silently skipped (D-04).
 */
@Singleton
public class DefaultSyncService implements SyncService {

    private final ConfigService configService;

    @Inject
    public DefaultSyncService(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public SyncResult sync(String destinationName, boolean dryRun, boolean verbose)
        throws IOException {
        var config = configService.load();

        // Find destination by name (same pattern as GeneratorService)
        var dest = config.getDestinations().stream()
            .filter(d -> d.getName().equals(destinationName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Destination '" + destinationName + "' not found."));

        // Tilde expansion (Phase 11 D-10 pattern)
        var rawDestPath = dest.getPath();
        String resolvedDestPath = rawDestPath.startsWith("~")
            ? System.getProperty("user.home") + rawDestPath.substring(1)
            : rawDestPath;
        Path destRoot = Path.of(resolvedDestPath);

        // Accumulators
        var copied = 0;
        var skipped = 0;
        var failed = 0;
        var conflicts = 0;
        var verboseLines = new ArrayList<String>();
        var conflictRecords = new ArrayList<SyncConflictRecord>();

        for (MappingEntry mapping : dest.getMappings()) {
            // SYN-01: only process sync: true mappings
            if (!mapping.isSync()) {
                continue;
            }

            // Resolve source by sourceRef
            var source = config.getSources().stream()
                .filter(s -> s.getName().equals(mapping.getSourceRef()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Source '" + mapping.getSourceRef() + "' not found."));

            // D-04: silently skip template sources
            if (source.isTemplates()) {
                continue;
            }

            // Tilde expansion on source path
            var rawSourcePath = source.getPath();
            String resolvedSourcePath = rawSourcePath.startsWith("~")
                ? System.getProperty("user.home") + rawSourcePath.substring(1)
                : rawSourcePath;
            Path sourceRoot = Path.of(resolvedSourcePath);

            int maxDepth = mapping.isRecurse() ? Integer.MAX_VALUE : 1;

            // Walk source files (same pattern as GeneratorService)
            List<Path> files;
            try (var stream = Files.walk(sourceRoot, maxDepth)) {
                files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !HiddenPathFilter.hasHiddenPathSegment(sourceRoot, p))
                    .filter(p -> {
                        var glob = mapping.getGlob();
                        if (glob == null) return true;
                        return GlobMatcher.matches(glob, sourceRoot.relativize(p));
                    })
                    .sorted()
                    .collect(Collectors.toList());
            }

            for (Path sourcePath : files) {
                var relPath = sourceRoot.relativize(sourcePath);
                var destPath = destRoot.resolve(relPath);

                try {
                    FileTime srcMtime = Files.getLastModifiedTime(sourcePath);

                    if (!Files.exists(destPath)) {
                        // New file — always copy
                        if (!dryRun) {
                            Files.createDirectories(destPath.getParent());
                            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                        copied++;
                        if (verbose) {
                            verboseLines.add("Copied " + destPath);
                        }
                    } else {
                        FileTime dstMtime = Files.getLastModifiedTime(destPath);
                        var cmp = srcMtime.compareTo(dstMtime);
                        var mismatch = Files.mismatch(sourcePath, destPath);
                        var contentIdentical = (mismatch == -1L);

                        if (contentIdentical) {
                            // Content identical regardless of mtime — safe to skip (D-03)
                            skipped++;
                            if (verbose) {
                                verboseLines.add("Skipped " + destPath);
                            }
                        } else if (cmp >= 0) {
                            // Source same-age or newer AND content differs — copy (update)
                            if (!dryRun) {
                                Files.createDirectories(destPath.getParent());
                                Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                            copied++;
                            if (verbose) {
                                verboseLines.add("Copied " + destPath);
                            }
                        } else {
                            // Destination newer AND content differs — CONFLICT (D-01)
                            conflicts++;
                            var posixRelPath = relPath.toString().replace('\\', '/');
                            conflictRecords.add(new SyncConflictRecord(
                                posixRelPath,
                                SyncConflictKind.CONTENT_MISMATCH,
                                "destination newer than source"));
                            if (verbose) {
                                verboseLines.add("Conflict " + destPath);
                            }
                        }
                    }
                } catch (IOException e) {
                    failed++;
                    if (verbose) {
                        verboseLines.add("Failed " + destPath + " (" + e.getMessage() + ")");
                    }
                }
            }
        }

        return new SyncResult(copied, skipped, failed, conflicts,
            Collections.unmodifiableList(verboseLines),
            Collections.unmodifiableList(conflictRecords));
    }
}
