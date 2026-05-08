package org.saltations.sync;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigService;
import org.saltations.model.CatalogEntry;
import org.saltations.model.ProjectEntry;
import org.saltations.model.SubscriptionEntry;
import org.saltations.model.ViracochaConfig;
import org.saltations.infra.HiddenPathFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Filesystem sync engine: one-way and bidirectional subscription sync with conflict detection.
 */
@Singleton
public class DefaultSyncService implements SyncService {

    private final ConfigService configService;

    @Inject
    public DefaultSyncService(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public SyncEngineResult syncProject(String projectName, String subscriptionIdOrNull, boolean dryRun, boolean verbose)
        throws IOException {
        ViracochaConfig config = configService.load();
        ProjectEntry project = config.getProjects().stream()
            .filter(p -> p.getName().equals(projectName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Project '" + projectName + "' not found."));

        List<SubscriptionEntry> toProcess = resolveSubscriptions(project, projectName, subscriptionIdOrNull);

        SyncEngineResult engine = new SyncEngineResult();
        List<SyncSubscriptionResult> results = new ArrayList<>();
        for (SubscriptionEntry sub : toProcess) {
            results.add(syncSubscription(config, project, sub, dryRun, verbose));
        }
        engine.setSubscriptionResults(results);
        return engine;
    }

    private static List<SubscriptionEntry> resolveSubscriptions(
        ProjectEntry project,
        String projectName,
        String subscriptionIdOrNull
    ) {
        if (subscriptionIdOrNull == null) {
            return project.getSubscriptions();
        }
        List<SubscriptionEntry> filtered = project.getSubscriptions().stream()
            .filter(s -> subscriptionIdOrNull.equals(s.getId()))
            .toList();
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException(
                "Subscription '" + subscriptionIdOrNull + "' not found for project '" + projectName + "'.");
        }
        return filtered;
    }

    private SyncSubscriptionResult syncSubscription(
        ViracochaConfig config,
        ProjectEntry project,
        SubscriptionEntry sub,
        boolean dryRun,
        boolean verbose
    ) throws IOException {
        CatalogEntry cat = config.getCatalogs().stream()
            .filter(c -> c.getName().equals(sub.getCatalogName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Catalog '" + sub.getCatalogName() + "' not found."));

        ResolvedRoots roots = resolveRoots(cat, project, sub);

        return switch (sub.getDirection()) {
            case BIDIRECTIONAL -> syncBidirectional(sub.getId(), roots, dryRun, verbose);
            case CATALOG_TO_WORKSPACE -> syncOneWay(sub.getId(), roots.catalogSubtree(), roots.workspaceSubtree(), dryRun, verbose);
            case WORKSPACE_TO_CATALOG -> syncOneWay(sub.getId(), roots.workspaceSubtree(), roots.catalogSubtree(), dryRun, verbose);
        };
    }

    private record ResolvedRoots(Path catalogSubtree, Path workspaceSubtree) {}

    private static ResolvedRoots resolveRoots(CatalogEntry cat, ProjectEntry proj, SubscriptionEntry sub) {
        Path catalogSubtree = Path.of(cat.getPath()).resolve(sub.getSourcePath()).normalize().toAbsolutePath();
        Path workspaceSubtree = Path.of(proj.getPath()).resolve(sub.getWorkspacePath()).normalize().toAbsolutePath();
        return new ResolvedRoots(catalogSubtree, workspaceSubtree);
    }

    /**
     * One-way sync: copy from {@code sourceRoot} to {@code destRoot} for every regular file under source.
     */
    private SyncSubscriptionResult syncOneWay(
        String subscriptionId,
        Path sourceRoot,
        Path destRoot,
        boolean dryRun,
        boolean verbose
    ) throws IOException {
        SyncSubscriptionResult result = baseResult(subscriptionId);
        if (!Files.exists(sourceRoot)) {
            result.setSuccess(true);
            return result;
        }

        List<Path> sortedPaths = collectSortedCandidatePaths(sourceRoot);
        for (Path sourcePath : sortedPaths) {
            String rel = toPosixRel(sourceRoot, sourcePath);
            Path destPath = destRoot.resolve(rel).normalize();

            if (Files.isSymbolicLink(sourcePath)) {
                addConflict(result, subscriptionId, rel, SyncConflictKind.SYMLINK_UNSUPPORTED, verbose, "BLOCKED ");
                continue;
            }
            if (!Files.isRegularFile(sourcePath)) {
                continue;
            }

            if (Files.isSymbolicLink(destPath)) {
                addConflict(result, subscriptionId, rel, SyncConflictKind.SYMLINK_UNSUPPORTED, verbose, "BLOCKED ");
                continue;
            }

            if (!Files.exists(destPath)) {
                if (!dryRun) {
                    Files.createDirectories(destPath.getParent());
                    Files.copy(sourcePath, destPath, StandardCopyOption.COPY_ATTRIBUTES);
                }
                result.setFilesCopied(result.getFilesCopied() + 1);
                verboseLine(result, verbose, "COPY " + rel);
                continue;
            }

            if (Files.isDirectory(destPath)) {
                addConflict(result, subscriptionId, rel, SyncConflictKind.TYPE_MISMATCH, verbose, "CONFLICT ");
                continue;
            }

            if (Files.isRegularFile(destPath)) {
                long mismatch = Files.mismatch(sourcePath, destPath);
                if (mismatch == -1L) {
                    result.setFilesSkipped(result.getFilesSkipped() + 1);
                    verboseLine(result, verbose, "SKIP " + rel);
                } else {
                    addConflict(result, subscriptionId, rel, SyncConflictKind.CONTENT_MISMATCH, verbose, "CONFLICT ");
                }
            }
        }

        finalizeResult(result);
        return result;
    }

    private SyncSubscriptionResult syncBidirectional(String subscriptionId, ResolvedRoots roots, boolean dryRun, boolean verbose)
        throws IOException {
        Path catRoot = roots.catalogSubtree();
        Path wsRoot = roots.workspaceSubtree();

        Set<String> union = new TreeSet<>();
        collectFileRelPaths(catRoot, union);
        collectFileRelPaths(wsRoot, union);

        List<SyncConflictRecord> analyzeConflicts = new ArrayList<>();
        for (String rel : union) {
            Path pCat = catRoot.resolve(rel).normalize();
            Path pWs = wsRoot.resolve(rel).normalize();
            classifyForBidirectional(subscriptionId, rel, pCat, pWs).ifPresent(analyzeConflicts::add);
        }

        if (!analyzeConflicts.isEmpty()) {
            SyncSubscriptionResult result = baseResult(subscriptionId);
            result.getConflictRecords().addAll(analyzeConflicts);
            result.setConflicts(analyzeConflicts.size());
            result.setSuccess(false);
            for (SyncConflictRecord rec : analyzeConflicts) {
                verboseLine(result, verbose, "CONFLICT " + rec.getRelativePath() + " " + rec.getKind());
            }
            return result;
        }

        SyncSubscriptionResult result = baseResult(subscriptionId);

        // Apply phase — catalog → workspace first (D-08)
        for (String rel : union) {
            Path pCat = catRoot.resolve(rel).normalize();
            Path pWs = wsRoot.resolve(rel).normalize();
            if (!Files.exists(pCat) || Files.isSymbolicLink(pCat) || !Files.isRegularFile(pCat)) {
                continue;
            }
            if (!Files.exists(pWs)) {
                if (!dryRun) {
                    Files.createDirectories(pWs.getParent());
                    Files.copy(pCat, pWs, StandardCopyOption.COPY_ATTRIBUTES);
                }
                result.setFilesCopied(result.getFilesCopied() + 1);
                verboseLine(result, verbose, "COPY " + rel);
            } else if (Files.isRegularFile(pWs) && Files.mismatch(pCat, pWs) == -1L) {
                result.setFilesSkipped(result.getFilesSkipped() + 1);
                verboseLine(result, verbose, "SKIP " + rel);
            }
        }

        // Second pass — workspace → catalog (paths still missing on catalog only)
        for (String rel : union) {
            Path pCat = catRoot.resolve(rel).normalize();
            Path pWs = wsRoot.resolve(rel).normalize();
            if (!Files.exists(pWs) || Files.isSymbolicLink(pWs) || !Files.isRegularFile(pWs)) {
                continue;
            }
            if (Files.exists(pCat)) {
                continue;
            }
            if (!dryRun) {
                Files.createDirectories(pCat.getParent());
                Files.copy(pWs, pCat, StandardCopyOption.COPY_ATTRIBUTES);
            }
            result.setFilesCopied(result.getFilesCopied() + 1);
            verboseLine(result, verbose, "COPY " + rel);
        }

        finalizeResult(result);
        return result;
    }

    private Optional<SyncConflictRecord> classifyForBidirectional(String subscriptionId, String rel, Path pCat, Path pWs)
        throws IOException {
        boolean exCat = Files.exists(pCat);
        boolean exWs = Files.exists(pWs);
        if (!exCat || !exWs) {
            return Optional.empty();
        }
        if (Files.isSymbolicLink(pCat) || Files.isSymbolicLink(pWs)) {
            return Optional.of(new SyncConflictRecord(subscriptionId, rel, SyncConflictKind.SYMLINK_UNSUPPORTED, null));
        }
        boolean regCat = Files.isRegularFile(pCat);
        boolean regWs = Files.isRegularFile(pWs);
        if (regCat != regWs) {
            return Optional.of(new SyncConflictRecord(subscriptionId, rel, SyncConflictKind.TYPE_MISMATCH, null));
        }
        if (!regCat) {
            return Optional.empty();
        }
        long mismatch = Files.mismatch(pCat, pWs);
        if (mismatch == -1L) {
            return Optional.empty();
        }
        return Optional.of(new SyncConflictRecord(subscriptionId, rel, SyncConflictKind.CONTENT_MISMATCH, null));
    }

    private static void collectFileRelPaths(Path root, Set<String> out) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> paths = walk
                .filter(p -> !p.equals(root))
                .filter(p -> !HiddenPathFilter.hasHiddenPathSegment(root, p))
                .toList();
            for (Path p : paths) {
                if (Files.isSymbolicLink(p) || Files.isRegularFile(p)) {
                    out.add(toPosixRel(root, p));
                }
            }
        }
    }

    private static List<Path> collectSortedCandidatePaths(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk
                .filter(p -> !p.equals(root))
                .filter(p -> !HiddenPathFilter.hasHiddenPathSegment(root, p))
                .filter(p -> {
                    try {
                        return Files.isSymbolicLink(p) || Files.isRegularFile(p);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .sorted(Comparator.comparing(p -> toPosixRel(root, p)))
                .toList();
        }
    }

    private static String toPosixRel(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static SyncSubscriptionResult baseResult(String subscriptionId) {
        SyncSubscriptionResult r = new SyncSubscriptionResult();
        r.setSubscriptionId(subscriptionId);
        r.setConflictRecords(new ArrayList<>());
        r.setVerboseLines(new ArrayList<>());
        r.setFilesCopied(0);
        r.setFilesSkipped(0);
        r.setFilesFailed(0);
        r.setConflicts(0);
        return r;
    }

    private static void verboseLine(SyncSubscriptionResult result, boolean verbose, String line) {
        if (verbose) {
            result.getVerboseLines().add(line);
        }
    }

    private static void addConflict(
        SyncSubscriptionResult result,
        String subscriptionId,
        String rel,
        SyncConflictKind kind,
        boolean verbose,
        String prefix
    ) {
        result.getConflictRecords().add(new SyncConflictRecord(subscriptionId, rel, kind, null));
        result.setConflicts(result.getConflicts() + 1);
        verboseLine(result, verbose, prefix + rel + " " + kind);
    }

    private static void finalizeResult(SyncSubscriptionResult result) {
        result.setSuccess(result.getConflicts() == 0 && result.getErrorMessage() == null);
    }
}
