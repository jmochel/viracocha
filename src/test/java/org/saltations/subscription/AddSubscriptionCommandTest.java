package org.saltations.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.XdgPaths;
import org.saltations.model.ProjectEntry;
import org.saltations.model.CatalogEntry;
import org.saltations.model.SubscriptionSyncDirection;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link AddSubscriptionCommand} with isolated config.
 */
class AddSubscriptionCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
    private ConfigService configService;

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
        AddSubscriptionCommand command = new AddSubscriptionCommand(configService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    private void seedProjectAndCatalog() throws Exception {
        Path pubDir = Files.createDirectory(tempDir.resolve("cat"));
        Path ws = Files.createDirectory(tempDir.resolve("ws"));
        ViracochaConfig cfg = configService.load();
        cfg.getCatalogs().add(new CatalogEntry("pub1", pubDir.toString()));
        cfg.getProjects().add(new ProjectEntry("p1", ws.toString(), new ArrayList<>(), new LinkedHashMap<>(), new ArrayList<>()));
        configService.save(cfg);
    }

    @Test
    void addPersistsSubscription() throws Exception {
        seedProjectAndCatalog();
        int exit = commandLine.execute(
            "--project", "p1",
            "--catalog", "pub1",
            "--source", "src",
            "--workspace", "out",
            "--direction", "catalog-to-workspace");
        assertEquals(0, exit);
        assertTrue(stdout.toString().contains("Subscription added"));
        ViracochaConfig cfg = configService.load();
        assertEquals(1, cfg.getProjects().get(0).getSubscriptions().size());
        var sub = cfg.getProjects().get(0).getSubscriptions().get(0);
        assertEquals("pub1", sub.getCatalogName());
        assertEquals(SubscriptionSyncDirection.CATALOG_TO_WORKSPACE, sub.getDirection());
    }

    @Test
    void unknownProjectExitsOne() throws Exception {
        seedProjectAndCatalog();
        int exit = commandLine.execute(
            "--project", "nope",
            "--catalog", "pub1",
            "--source", "a",
            "--workspace", "b",
            "--direction", "catalog-to-workspace");
        assertEquals(1, exit);
        assertTrue(stderr.toString().contains("not found"));
    }
}
