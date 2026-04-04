package org.saltations.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.XdgPaths;
import org.saltations.model.ProjectEntry;
import org.saltations.model.PublisherEntry;
import org.saltations.model.SubscriptionEntry;
import org.saltations.model.SubscriptionSyncDirection;
import org.saltations.model.ViracochaConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DefaultSyncServiceOneWayTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private DefaultSyncService syncService;

    private XdgPaths xdgPaths() {
        return new XdgPaths() {
            @Override
            public Path configFile() {
                return tempDir.resolve("viracocha/config.yaml");
            }

            @Override
            public Path configDir() {
                return tempDir.resolve("viracocha");
            }

            @Override
            public Path dataDir() {
                return tempDir.resolve("share/viracocha");
            }
        };
    }

    @BeforeEach
    void setUp() throws Exception {
        configService = new ConfigService(xdgPaths());
        configService.init();
        syncService = new DefaultSyncService(configService);
    }

    @Test
    void publishToWorkspace_copiesFileFromPublisherToWorkspace() throws Exception {
        Path pubRoot = Files.createDirectories(tempDir.resolve("publisher"));
        Path wsRoot = Files.createDirectories(tempDir.resolve("workspace"));
        Files.createDirectories(pubRoot.resolve("src/sub"));
        Files.writeString(pubRoot.resolve("src/sub/a.txt"), "hello", StandardCharsets.UTF_8);

        saveConfig(pubRoot, wsRoot, SubscriptionSyncDirection.PUBLISH_TO_WORKSPACE, "src", "out");

        SyncEngineResult r = syncService.syncProject("proj1");
        SyncSubscriptionResult sub = single(r);
        assertTrue(sub.isSuccess());
        assertTrue(sub.getFilesCopied() >= 1);

        Path copied = wsRoot.resolve("out/sub/a.txt");
        assertTrue(Files.isRegularFile(copied));
        assertEquals("hello", Files.readString(copied, StandardCharsets.UTF_8));
    }

    @Test
    void workspaceToPublish_copiesFileFromWorkspaceToPublisher() throws Exception {
        Path pubRoot = Files.createDirectories(tempDir.resolve("publisher"));
        Path wsRoot = Files.createDirectories(tempDir.resolve("workspace"));
        Files.createDirectories(wsRoot.resolve("out/sub"));
        Files.writeString(wsRoot.resolve("out/sub/w.txt"), "ws-only", StandardCharsets.UTF_8);

        saveConfig(pubRoot, wsRoot, SubscriptionSyncDirection.WORKSPACE_TO_PUBLISH, "src", "out");

        SyncEngineResult r = syncService.syncProject("proj1");
        SyncSubscriptionResult sub = single(r);
        assertTrue(sub.isSuccess());
        assertTrue(sub.getFilesCopied() >= 1);

        Path copied = pubRoot.resolve("src/sub/w.txt");
        assertTrue(Files.isRegularFile(copied));
        assertEquals("ws-only", Files.readString(copied, StandardCharsets.UTF_8));
    }

    @Test
    void contentMismatch_doesNotOverwriteDestination() throws Exception {
        Path pubRoot = Files.createDirectories(tempDir.resolve("publisher"));
        Path wsRoot = Files.createDirectories(tempDir.resolve("workspace"));
        Files.createDirectories(pubRoot.resolve("src/sub"));
        Files.writeString(pubRoot.resolve("src/sub/x.txt"), "aaa", StandardCharsets.UTF_8);
        Files.createDirectories(wsRoot.resolve("out/sub"));
        Files.writeString(wsRoot.resolve("out/sub/x.txt"), "bbb", StandardCharsets.UTF_8);

        saveConfig(pubRoot, wsRoot, SubscriptionSyncDirection.PUBLISH_TO_WORKSPACE, "src", "out");

        SyncEngineResult r = syncService.syncProject("proj1");
        SyncSubscriptionResult sub = single(r);
        assertFalse(sub.isSuccess());
        assertTrue(sub.getConflictRecords().stream()
            .anyMatch(c -> c.getKind() == SyncConflictKind.CONTENT_MISMATCH));
        assertEquals("bbb", Files.readString(wsRoot.resolve("out/sub/x.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void hiddenPathSegment_isNotPropagated() throws Exception {
        Path pubRoot = Files.createDirectories(tempDir.resolve("publisher"));
        Path wsRoot = Files.createDirectories(tempDir.resolve("workspace"));
        Files.createDirectories(pubRoot.resolve("src/.secret"));
        Files.writeString(pubRoot.resolve("src/.secret/h.txt"), "hidden", StandardCharsets.UTF_8);

        saveConfig(pubRoot, wsRoot, SubscriptionSyncDirection.PUBLISH_TO_WORKSPACE, "src", "out");

        SyncEngineResult r = syncService.syncProject("proj1");
        SyncSubscriptionResult sub = single(r);
        assertTrue(sub.isSuccess());
        assertFalse(Files.exists(wsRoot.resolve("out/.secret/h.txt")));
    }

    private void saveConfig(
        Path pubRoot,
        Path wsRoot,
        SubscriptionSyncDirection direction,
        String sourcePath,
        String destPath
    ) throws Exception {
        ViracochaConfig cfg = new ViracochaConfig();
        cfg.getPublishers().add(new PublisherEntry("pub1", pubRoot.toString()));
        SubscriptionEntry sub = new SubscriptionEntry(
            "11111111-1111-1111-1111-111111111111",
            "pub1",
            sourcePath,
            destPath,
            direction);
        cfg.getProjects().add(new ProjectEntry("proj1", wsRoot.toString(), List.of(), new LinkedHashMap<>(), List.of(sub)));
        configService.save(cfg);
    }

    private static SyncSubscriptionResult single(SyncEngineResult r) {
        List<SyncSubscriptionResult> list = r.getSubscriptionResults();
        assertEquals(1, list.size());
        return list.get(0);
    }
}
