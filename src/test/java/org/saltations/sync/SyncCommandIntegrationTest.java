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
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end {@link SyncCommand} with temp config and real filesystem paths.
 */
class SyncCommandIntegrationTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;

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
        SyncCommand cmd = new SyncCommand(new DefaultSyncService(configService));
        commandLine = new CommandLine(cmd);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void syncPublisherToWorkspace_copiesViaCli() throws Exception {
        Path pubRoot = Files.createDirectories(tempDir.resolve("publisher"));
        Path wsRoot = Files.createDirectories(tempDir.resolve("workspace"));
        Files.createDirectories(pubRoot.resolve("src/sub"));
        Files.writeString(pubRoot.resolve("src/sub/a.txt"), "hello", StandardCharsets.UTF_8);

        saveConfig(pubRoot, wsRoot, SubscriptionSyncDirection.PUBLISH_TO_WORKSPACE, "src", "out",
            "33333333-3333-3333-3333-333333333333");

        int exit = commandLine.execute("--project-name", "intproj");
        assertEquals(0, exit, stderr.toString());
        String out = stdout.toString();
        assertTrue(out.contains("Copied:"));
        assertTrue(out.contains("Copied: 1") || out.matches("(?s).*Copied: [1-9]\\d*.*"));
        Path copied = wsRoot.resolve("out/sub/a.txt");
        assertTrue(Files.isRegularFile(copied));
        assertEquals("hello", Files.readString(copied, StandardCharsets.UTF_8));
    }

    @Test
    void syncWorkspaceToPublisher_copiesViaCli() throws Exception {
        Path pubRoot = Files.createDirectories(tempDir.resolve("publisher"));
        Path wsRoot = Files.createDirectories(tempDir.resolve("workspace"));
        Files.createDirectories(wsRoot.resolve("out/sub"));
        Files.writeString(wsRoot.resolve("out/sub/w.txt"), "ws-only", StandardCharsets.UTF_8);

        saveConfig(pubRoot, wsRoot, SubscriptionSyncDirection.WORKSPACE_TO_PUBLISH, "src", "out",
            "44444444-4444-4444-4444-444444444444");

        int exit = commandLine.execute("--project-name", "intproj");
        assertEquals(0, exit, stderr.toString());
        Path copied = pubRoot.resolve("src/sub/w.txt");
        assertTrue(Files.isRegularFile(copied));
        assertEquals("ws-only", Files.readString(copied, StandardCharsets.UTF_8));
    }

    @Test
    void bidirectionalConflictExitsNonZero() throws Exception {
        Path pubRoot = Files.createDirectories(tempDir.resolve("publisher"));
        Path wsRoot = Files.createDirectories(tempDir.resolve("workspace"));
        Files.createDirectories(pubRoot.resolve("src"));
        Files.createDirectories(wsRoot.resolve("out"));
        Files.writeString(pubRoot.resolve("src/c.txt"), "left", StandardCharsets.UTF_8);
        Files.writeString(wsRoot.resolve("out/c.txt"), "right", StandardCharsets.UTF_8);

        saveConfig(pubRoot, wsRoot, SubscriptionSyncDirection.BIDIRECTIONAL, "src", "out",
            "55555555-5555-5555-5555-555555555555");

        int exit = commandLine.execute("--project-name", "intproj");
        assertEquals(1, exit);
        String err = stderr.toString();
        String out = stdout.toString();
        assertTrue(out.contains("Conflicts:") && out.contains("1")
            || err.contains("CONFLICT")
            || out.contains("CONFLICT"));
    }

    private void saveConfig(
        Path pubRoot,
        Path wsRoot,
        SubscriptionSyncDirection direction,
        String sourcePath,
        String destPath,
        String subscriptionId
    ) throws Exception {
        ViracochaConfig cfg = new ViracochaConfig();
        cfg.getPublishers().add(new PublisherEntry("pub1", pubRoot.toString()));
        SubscriptionEntry sub = new SubscriptionEntry(
            subscriptionId,
            "pub1",
            sourcePath,
            destPath,
            direction);
        cfg.getProjects().add(new ProjectEntry("intproj", wsRoot.toString(), List.of(), new LinkedHashMap<>(), List.of(sub)));
        configService.save(cfg);
    }
}
