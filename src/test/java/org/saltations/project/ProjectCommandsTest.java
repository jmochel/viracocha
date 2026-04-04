package org.saltations.project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.XdgPaths;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectCommandsTest {

    @TempDir Path tempDir;
    private ConfigService configService;

    private XdgPaths xdgPaths() {
        return new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("viracocha/config.yaml"); }
            @Override public Path configDir() { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir() { return tempDir.resolve("share/viracocha"); }
        };
    }

    @BeforeEach
    void setUp() throws Exception {
        configService = new ConfigService(xdgPaths());
        configService.init();
    }

    @Test
    void createListUnregisterFlow() throws Exception {
        Path ws = Files.createDirectory(tempDir.resolve("ws"));

        CreateProjectCommand create = new CreateProjectCommand(configService);
        CommandLine clCreate = new CommandLine(create);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        clCreate.setOut(new PrintWriter(out, true));
        clCreate.setErr(new PrintWriter(err, true));
        assertEquals(0, clCreate.execute("--name", "p1", "--path", ws.toString()));
        assertTrue(out.toString().contains("Project 'p1' created."));

        ListProjectsCommand list = new ListProjectsCommand(configService);
        CommandLine clList = new CommandLine(list);
        out.reset();
        clList.setOut(new PrintWriter(out, true));
        clList.setErr(new PrintWriter(err, true));
        assertEquals(0, clList.execute());
        assertTrue(out.toString().contains("p1"));
        assertTrue(out.toString().contains(ws.toString()));

        UnregisterProjectCommand unreg = new UnregisterProjectCommand(configService);
        CommandLine clUn = new CommandLine(unreg);
        out.reset();
        clUn.setOut(new PrintWriter(out, true));
        clUn.setErr(new PrintWriter(err, true));
        assertEquals(0, clUn.execute("--name", "p1"));
        assertTrue(out.toString().contains("unregistered"));

        ViracochaConfig cfg = configService.load();
        assertTrue(cfg.getProjects().isEmpty());
    }

    @Test
    void duplicateCreateExitsOne() throws Exception {
        Path ws = Files.createDirectory(tempDir.resolve("ws"));
        CreateProjectCommand create = new CreateProjectCommand(configService);
        CommandLine cl = new CommandLine(create);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        cl.setOut(new PrintWriter(new ByteArrayOutputStream(), true));
        cl.setErr(new PrintWriter(err, true));
        cl.execute("--name", "p1", "--path", ws.toString());
        err.reset();
        int exit = cl.execute("--name", "p1", "--path", ws.toString());
        assertEquals(1, exit);
        assertTrue(err.toString().contains("already exists"));
    }

    @Test
    void unregisterMissingExitsOne() throws Exception {
        UnregisterProjectCommand unreg = new UnregisterProjectCommand(configService);
        CommandLine cl = new CommandLine(unreg);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        cl.setOut(new PrintWriter(new ByteArrayOutputStream(), true));
        cl.setErr(new PrintWriter(err, true));
        int exit = cl.execute("--name", "nope");
        assertEquals(1, exit);
        assertTrue(err.toString().contains("not found"));
    }
}
