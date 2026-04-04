package org.saltations.pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.XdgPaths;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ListPatternsCommandTest {

    @TempDir Path tempDir;
    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;
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
        ListPatternsCommand command = new ListPatternsCommand(configService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void listShowsNamePathAndParamCount() throws Exception {
        Path patDir = Files.createDirectory(tempDir.resolve("mypat"));
        Files.writeString(patDir.resolve("t.txt"), "${name} ${email}", StandardCharsets.UTF_8);
        new CommandLine(new RegisterPatternCommand(configService))
            .execute("--name", "mypat", "--path", patDir.toString());

        int exit = commandLine.execute();
        assertEquals(0, exit);
        String out = stdout.toString();
        assertTrue(out.contains("mypat"), "Output must contain pattern name");
        assertTrue(out.contains("2"), "Output must contain param count (2)");
    }

    @Test
    void listWithJsonOutputsJsonWithParametersField() throws Exception {
        Path patDir = Files.createDirectory(tempDir.resolve("mypat"));
        Files.writeString(patDir.resolve("t.txt"), "${name}", StandardCharsets.UTF_8);
        new CommandLine(new RegisterPatternCommand(configService))
            .execute("--name", "mypat", "--path", patDir.toString());

        int exit = commandLine.execute("--json");
        assertEquals(0, exit);
        String out = stdout.toString();
        assertTrue(out.contains("\"name\""), "JSON must have 'name' field");
        assertTrue(out.contains("\"path\""), "JSON must have 'path' field");
        assertTrue(out.contains("\"parameters\""), "JSON must have 'parameters' field");
    }

    @Test
    void listEmptyExitsZeroWithNoOutput() {
        int exit = commandLine.execute();
        assertEquals(0, exit);
        assertEquals("", stdout.toString().trim());
    }

    @Test
    void listWhenConfigNotInitializedExitsOne() {
        XdgPaths uninitXdg = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("uninit/config.yaml"); }
            @Override public Path configDir() { return tempDir.resolve("uninit"); }
            @Override public Path dataDir() { return tempDir.resolve("uninit/share"); }
        };
        ListPatternsCommand cmd = new ListPatternsCommand(new ConfigService(uninitXdg));
        CommandLine cl = new CommandLine(cmd);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        cl.setErr(new PrintWriter(err, true));
        int exit = cl.execute();
        assertEquals(1, exit);
        assertTrue(err.toString().contains("Config not initialized"));
    }
}
