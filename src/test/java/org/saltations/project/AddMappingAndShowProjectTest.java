package org.saltations.project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.XdgPaths;
import org.saltations.model.PatternEntry;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AddMappingAndShowProjectTest {

    @TempDir Path tempDir;
    private ConfigService configService;

    @BeforeEach
    void setUp() throws Exception {
        XdgPaths xdgPaths = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("viracocha/config.yaml"); }
            @Override public Path configDir() { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir() { return tempDir.resolve("share/viracocha"); }
        };
        configService = new ConfigService(xdgPaths);
        configService.init();

        ViracochaConfig config = configService.load();
        config.getPatterns().add(new PatternEntry("pat1", "/tmp/pat", List.of("x")));
        configService.save(config);
    }

    @Test
    void addMappingShowAndJson() throws Exception {
        Path ws = Files.createDirectory(tempDir.resolve("ws"));

        CreateProjectCommand create = new CreateProjectCommand(configService);
        assertEquals(0, new CommandLine(create).execute("--name", "proj", "--path", ws.toString()));

        AddMappingCommand add = new AddMappingCommand(configService);
        CommandLine clAdd = new CommandLine(add);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        clAdd.setOut(new PrintWriter(new ByteArrayOutputStream(), true));
        clAdd.setErr(new PrintWriter(err, true));
        int exitAdd = clAdd.execute(
            "--project", "proj",
            "--pattern", "pat1",
            "--destination", "rel/out",
            "--param", "a=1",
            "--param", "b=two"
        );
        assertEquals(0, exitAdd, err.toString());

        ShowProjectCommand show = new ShowProjectCommand(configService);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CommandLine clShow = new CommandLine(show);
        clShow.setOut(new PrintWriter(out, true));
        clShow.setErr(new PrintWriter(err, true));
        assertEquals(0, clShow.execute("--name", "proj"));
        String plain = out.toString();
        assertTrue(plain.contains("pat1"));
        assertTrue(plain.contains("rel/out"));
        assertTrue(plain.contains("a=1") || plain.contains("a") && plain.contains("1"));

        out.reset();
        assertEquals(0, clShow.execute("--name", "proj", "--json"));
        assertTrue(out.toString().contains("\"name\":\"proj\""));
        assertTrue(out.toString().contains("pat1"));
    }

    @Test
    void addMappingWithUnknownPatternExitsOneAndDoesNotSave() throws Exception {
        Path ws = Files.createDirectory(tempDir.resolve("ws2"));
        CreateProjectCommand create = new CreateProjectCommand(configService);
        assertEquals(0, new CommandLine(create).execute("--name", "p2", "--path", ws.toString()));

        AddMappingCommand add = new AddMappingCommand(configService);
        CommandLine clAdd = new CommandLine(add);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        clAdd.setOut(new PrintWriter(new ByteArrayOutputStream(), true));
        clAdd.setErr(new PrintWriter(err, true));
        int exit = clAdd.execute(
            "--project", "p2",
            "--pattern", "missing-pattern",
            "--destination", "x"
        );
        assertEquals(1, exit);
        assertTrue(err.toString().contains("Pattern 'missing-pattern' not found"));

        ViracochaConfig cfg = configService.load();
        assertTrue(cfg.getProjects().stream()
            .filter(p -> p.getName().equals("p2"))
            .findFirst()
            .orElseThrow()
            .getMappings()
            .isEmpty());
    }
}
