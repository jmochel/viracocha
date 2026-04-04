package org.saltations.sync;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigService;
import org.saltations.model.ProjectEntry;
import org.saltations.model.PublisherEntry;
import org.saltations.model.SubscriptionEntry;
import org.saltations.model.ViracochaConfig;
import org.saltations.pattern.PatternPathUtils;

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
    public SyncEngineResult syncProject(String projectName) throws IOException {
        ViracochaConfig config = configService.load();
        ProjectEntry project = config.getProjects().stream()
            .filter(p -> p.getName().equals(projectName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Project '" + projectName + "' not found."));

        SyncEngineResult engine = new SyncEngineResult();
        List<SyncSubscriptionResult> results = new ArrayList<>();
        for (SubscriptionEntry sub : project.getSubscriptions()) {
            results.add(syncSubscription(config, project, sub));
        }
        engine.setSubscriptionResults(results);
        return engine;
    }

    private SyncSubscriptionResult syncSubscription(ViracochaConfig config, ProjectEntry project, SubscriptionEntry sub)
        throws IOException {
        PublisherEntry pub = config.getPublishers().stream()
            .filter(p -> p.getName().equals(sub.getPublisherName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Publisher '" + sub.getPublisherName() + "' not found."));

        ResolvedRoots roots = resolveRoots(pub, project, sub);

        return switch (sub.getDirection()) {
            case BIDIRECTIONAL -> syncBidirectional(sub.getId(), roots);
            case PUBLISH_TO_WORKSPACE -> syncOneWay(sub.getId(), roots.publisherSubtree(), roots.workspaceSubtree());
            case WORKSPACE_TO_PUBLISH -> syncOneWay(sub.getId(), roots.workspaceSubtree(), roots.publisherSubtree());
        };
    }

    private record ResolvedRoots(Path publisherSubtree, Path workspaceSubtree) {}

    private static ResolvedRoots resolveRoots(PublisherEntry pub, ProjectEntry proj, SubscriptionEntry sub) {
        Path publisherSubtree = Path.of(pub.getPath()).resolve(sub.getSourcePath()).normalize().toAbsolutePath();
        Path workspaceSubtree = Path.of(proj.getPath()).resolve(sub.getDestinationPath()).normalize().toAbsolutePath();
        return new ResolvedRoots(publisherSubtree, workspaceSubtree);
    }

    /**
     * One-way sync: copy from {@code sourceRoot} to {@code destRoot} for every regular file under source.
     */
    private SyncSubscriptionResult syncOneWay(String subscriptionId, Path sourceRoot, Path destRoot) throws IOException {
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
                addConflict(result, subscriptionId, rel, SyncConflictKind.SYMLINK_UNSUPPORTED);
                continue;
            }
            if (!Files.isRegularFile(sourcePath)) {
                continue;
            }

            if (Files.isSymbolicLink(destPath)) {
                addConflict(result, subscriptionId, rel, SyncConflictKind.SYMLINK_UNSUPPORTED);
                continue;
            }

            if (!Files.exists(destPath)) {
                Files.createDirectories(destPath.getParent());
                Files.copy(sourcePath, destPath, StandardCopyOption.COPY_ATTRIBUTES);
                result.setFilesCopied(result.getFilesCopied() + 1);
                continue;
            }

            if (Files.isDirectory(destPath)) {
                addConflict(result, subscriptionId, rel, SyncConflictKind.TYPE_MISMATCH);
                continue;
            }

            if (Files.isRegularFile(destPath)) {
                long mismatch = Files.mismatch(sourcePath, destPath);
                if (mismatch == -1L) {
                    result.setFilesSkipped(result.getFilesSkipped() + 1);
                } else {
                    addConflict(result, subscriptionId, rel, SyncConflictKind.CONTENT_MISMATCH);
                }
            }
        }

        finalizeResult(result);
        return result;
    }

    private SyncSubscriptionResult syncBidirectional(String subscriptionId, ResolvedRoots roots) throws IOException {
        Path pubRoot = roots.publisherSubtree();
        Path wsRoot = roots.workspaceSubtree();

        Set<String> union = new TreeSet<>();
        collectFileRelPaths(pubRoot, union);
        collectFileRelPaths(wsRoot, union);

        List<SyncConflictRecord> analyzeConflicts = new ArrayList<>();
        for (String rel : union) {
            Path pPub = pubRoot.resolve(rel).normalize();
            Path pWs = wsRoot.resolve(rel).normalize();
            classifyForBidirectional(subscriptionId, rel, pPub, pWs).ifPresent(analyzeConflicts::add);
        }

        if (!analyzeConflicts.isEmpty()) {
            SyncSubscriptionResult result = baseResult(subscriptionId);
            result.getConflictRecords().addAll(analyzeConflicts);
            result.setConflicts(analyzeConflicts.size());
            result.setSuccess(false);
            return result;
        }

        SyncSubscriptionResult result = baseResult(subscriptionId);

        // Apply phase — publisher → workspace first (D-08)
        for (String rel : union) {
            Path pPub = pubRoot.resolve(rel).normalize();
            Path pWs = wsRoot.resolve(rel).normalize();
            if (!Files.exists(pPub) || Files.isSymbolicLink(pPub) || !Files.isRegularFile(pPub)) {
                continue;
            }
            if (!Files.exists(pWs)) {
                Files.createDirectories(pWs.getParent());
                Files.copy(pPub, pWs, StandardCopyOption.COPY_ATTRIBUTES);
                result.setFilesCopied(result.getFilesCopied() + 1);
            } else if (Files.isRegularFile(pWs) && Files.mismatch(pPub, pWs) == -1L) {
                result.setFilesSkipped(result.getFilesSkipped() + 1);
            }
        }

        // Second pass — workspace → publisher (paths still missing on publisher only)
        for (String rel : union) {
            Path pPub = pubRoot.resolve(rel).normalize();
            Path pWs = wsRoot.resolve(rel).normalize();
            if (!Files.exists(pWs) || Files.isSymbolicLink(pWs) || !Files.isRegularFile(pWs)) {
                continue;
            }
            if (Files.exists(pPub)) {
                continue;
            }
            Files.createDirectories(pPub.getParent());
            Files.copy(pWs, pPub, StandardCopyOption.COPY_ATTRIBUTES);
            result.setFilesCopied(result.getFilesCopied() + 1);
        }

        finalizeResult(result);
        return result;
    }

    private Optional<SyncConflictRecord> classifyForBidirectional(String subscriptionId, String rel, Path pPub, Path pWs)
        throws IOException {
        boolean exPub = Files.exists(pPub);
        boolean exWs = Files.exists(pWs);
        if (!exPub || !exWs) {
            return Optional.empty();
        }
        if (Files.isSymbolicLink(pPub) || Files.isSymbolicLink(pWs)) {
            return Optional.of(new SyncConflictRecord(subscriptionId, rel, SyncConflictKind.SYMLINK_UNSUPPORTED, null));
        }
        boolean regPub = Files.isRegularFile(pPub);
        boolean regWs = Files.isRegularFile(pWs);
        if (regPub != regWs) {
            return Optional.of(new SyncConflictRecord(subscriptionId, rel, SyncConflictKind.TYPE_MISMATCH, null));
        }
        if (!regPub) {
            return Optional.empty();
        }
        long mismatch = Files.mismatch(pPub, pWs);
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
                .filter(p -> !PatternPathUtils.hasHiddenPathSegment(root, p))
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
                .filter(p -> !PatternPathUtils.hasHiddenPathSegment(root, p))
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
        r.setFilesCopied(0);
        r.setFilesSkipped(0);
        r.setFilesFailed(0);
        r.setConflicts(0);
        return r;
    }

    private static void addConflict(SyncSubscriptionResult result, String subscriptionId, String rel, SyncConflictKind kind) {
        result.getConflictRecords().add(new SyncConflictRecord(subscriptionId, rel, kind, null));
        result.setConflicts(result.getConflicts() + 1);
    }

    private static void finalizeResult(SyncSubscriptionResult result) {
        result.setSuccess(result.getConflicts() == 0 && result.getErrorMessage() == null);
    }
}
