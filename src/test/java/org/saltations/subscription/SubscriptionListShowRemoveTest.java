package org.saltations.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.XdgPaths;
import org.saltations.model.ProjectEntry;
import org.saltations.model.CatalogEntry;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionListShowRemoveTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private final ObjectMapper json = new ObjectMapper();

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
    }

    private String addSubscriptionAndGetId() throws Exception {
        Path pubDir = Files.createDirectory(tempDir.resolve("pub"));
        Path ws = Files.createDirectory(tempDir.resolve("ws"));
        ViracochaConfig cfg = configService.load();
        cfg.getCatalogs().add(new CatalogEntry("pub1", pubDir.toString()));
        cfg.getProjects().add(new ProjectEntry("p1", ws.toString(), new ArrayList<>(), new LinkedHashMap<>(), new ArrayList<>()));
        configService.save(cfg);

        AddSubscriptionCommand add = new AddSubscriptionCommand(configService);
        CommandLine clAdd = new CommandLine(add);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        clAdd.setOut(new PrintWriter(out, true));
        clAdd.setErr(new PrintWriter(new ByteArrayOutputStream(), true));
        int exit = clAdd.execute(
            "--project", "p1",
            "--catalog", "pub1",
            "--source", "src",
            "--workspace", "out",
            "--direction", "catalog-to-workspace");
        assertEquals(0, exit);
        String line = out.toString();
        Matcher m = Pattern.compile("id=([0-9a-f-]{36})").matcher(line);
        assertTrue(m.find(), line);
        return m.group(1);
    }

    @Test
    void listShowRemoveFlow() throws Exception {
        String id = addSubscriptionAndGetId();

        ListSubscriptionsCommand listCmd = new ListSubscriptionsCommand(configService);
        CommandLine clList = new CommandLine(listCmd);
        ByteArrayOutputStream listOut = new ByteArrayOutputStream();
        clList.setOut(new PrintWriter(listOut, true));
        clList.setErr(new PrintWriter(new ByteArrayOutputStream(), true));
        assertEquals(0, clList.execute());
        assertTrue(listOut.toString().contains(id));

        ShowSubscriptionCommand showCmd = new ShowSubscriptionCommand(configService);
        CommandLine clShow = new CommandLine(showCmd);
        ByteArrayOutputStream showOut = new ByteArrayOutputStream();
        clShow.setOut(new PrintWriter(showOut, true));
        clShow.setErr(new PrintWriter(new ByteArrayOutputStream(), true));
        assertEquals(0, clShow.execute("--id", id, "--json"));
        JsonNode node = json.readTree(showOut.toString().trim());
        assertEquals(id, node.get("id").asText());
        assertEquals("p1", node.get("project").asText());

        RemoveSubscriptionCommand rmCmd = new RemoveSubscriptionCommand(configService);
        CommandLine clRm = new CommandLine(rmCmd);
        clRm.setOut(new PrintWriter(new ByteArrayOutputStream(), true));
        clRm.setErr(new PrintWriter(new ByteArrayOutputStream(), true));
        assertEquals(0, clRm.execute("--id", id));

        ViracochaConfig cfg = configService.load();
        assertTrue(cfg.getProjects().get(0).getSubscriptions().isEmpty());
    }
}
