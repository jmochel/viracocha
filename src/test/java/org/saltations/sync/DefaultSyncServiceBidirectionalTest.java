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

class DefaultSyncServiceBidirectionalTest {

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
    void bidirectional_contentConflict_abortsApply() throws Exception {
        Path pubRoot = Files.createDirectories(tempDir.resolve("publisher"));
        Path wsRoot = Files.createDirectories(tempDir.resolve("workspace"));
        Files.createDirectories(pubRoot.resolve("src"));
        Files.createDirectories(wsRoot.resolve("out"));
        Files.writeString(pubRoot.resolve("src/c.txt"), "left", StandardCharsets.UTF_8);
        Files.writeString(wsRoot.resolve("out/c.txt"), "right", StandardCharsets.UTF_8);

        saveConfig(pubRoot, wsRoot);

        byte[] beforePub = Files.readAllBytes(pubRoot.resolve("src/c.txt"));
        byte[] beforeWs = Files.readAllBytes(wsRoot.resolve("out/c.txt"));

        SyncEngineResult r = syncService.syncProject("proj1");
        SyncSubscriptionResult sub = single(r);
        assertFalse(sub.isSuccess());
        assertTrue(sub.getConflictRecords().stream()
            .anyMatch(c -> c.getKind() == SyncConflictKind.CONTENT_MISMATCH));

        assertArrayEquals(beforePub, Files.readAllBytes(pubRoot.resolve("src/c.txt")));
        assertArrayEquals(beforeWs, Files.readAllBytes(wsRoot.resolve("out/c.txt")));
    }

    @Test
    void bidirectional_fillsBothSides() throws Exception {
        Path pubRoot = Files.createDirectories(tempDir.resolve("publisher"));
        Path wsRoot = Files.createDirectories(tempDir.resolve("workspace"));
        Files.createDirectories(pubRoot.resolve("src/a"));
        Files.createDirectories(wsRoot.resolve("out/b"));
        Files.writeString(pubRoot.resolve("src/a/only-pub.txt"), "p", StandardCharsets.UTF_8);
        Files.writeString(wsRoot.resolve("out/b/only-ws.txt"), "w", StandardCharsets.UTF_8);

        saveConfig(pubRoot, wsRoot);

        SyncEngineResult r = syncService.syncProject("proj1");
        SyncSubscriptionResult sub = single(r);
        assertTrue(sub.isSuccess());
        assertTrue(sub.getFilesCopied() >= 2);

        assertEquals("p", Files.readString(wsRoot.resolve("out/a/only-pub.txt"), StandardCharsets.UTF_8));
        assertEquals("w", Files.readString(pubRoot.resolve("src/b/only-ws.txt"), StandardCharsets.UTF_8));
    }

    private void saveConfig(Path pubRoot, Path wsRoot) throws Exception {
        ViracochaConfig cfg = new ViracochaConfig();
        cfg.getPublishers().add(new PublisherEntry("pub1", pubRoot.toString()));
        SubscriptionEntry sub = new SubscriptionEntry(
            "22222222-2222-2222-2222-222222222222",
            "pub1",
            "src",
            "out",
            SubscriptionSyncDirection.BIDIRECTIONAL);
        cfg.getProjects().add(new ProjectEntry("proj1", wsRoot.toString(), List.of(), new LinkedHashMap<>(), List.of(sub)));
        configService.save(cfg);
    }

    private static SyncSubscriptionResult single(SyncEngineResult r) {
        assertEquals(1, r.getSubscriptionResults().size());
        return r.getSubscriptionResults().get(0);
    }
}
