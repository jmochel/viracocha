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

class ShowPatternCommandTest {

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
        ShowPatternCommand command = new ShowPatternCommand(configService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void showExistingPatternPrintsNamePathAndParameters() throws Exception {
        Path patDir = Files.createDirectory(tempDir.resolve("mypat"));
        Files.writeString(patDir.resolve("t.txt"), "${name} ${email}", StandardCharsets.UTF_8);
        new CommandLine(new RegisterPatternCommand(configService))
            .execute("--name", "mypat", "--path", patDir.toString());

        int exit = commandLine.execute("--name", "mypat");
        assertEquals(0, exit);
        String out = stdout.toString();
        assertTrue(out.contains("Name: mypat"), "Must contain 'Name: mypat'");
        assertTrue(out.contains("Path: " + patDir), "Must contain 'Path: ...'");
        assertTrue(out.contains("Parameters:"), "Must contain 'Parameters:' label");
        assertTrue(out.contains("email") && out.contains("name"),
            "Parameters line must list extracted variable names");
    }

    @Test
    void showPatternWithNoParametersPrintsEmptyParametersLine() throws Exception {
        Path patDir = Files.createDirectory(tempDir.resolve("mypat"));
        Files.writeString(patDir.resolve("t.txt"), "no variables here", StandardCharsets.UTF_8);
        new CommandLine(new RegisterPatternCommand(configService))
            .execute("--name", "mypat", "--path", patDir.toString());

        int exit = commandLine.execute("--name", "mypat");
        assertEquals(0, exit);
        assertTrue(stdout.toString().contains("Parameters:"), "Must contain 'Parameters:' even when empty");
    }

    @Test
    void showWithJsonFlagOutputsSingleJsonObject() throws Exception {
        Path patDir = Files.createDirectory(tempDir.resolve("mypat"));
        Files.writeString(patDir.resolve("t.txt"), "${name}", StandardCharsets.UTF_8);
        new CommandLine(new RegisterPatternCommand(configService))
            .execute("--name", "mypat", "--path", patDir.toString());

        int exit = commandLine.execute("--name", "mypat", "--json");
        assertEquals(0, exit);
        String out = stdout.toString();
        assertTrue(out.contains("\"name\""), "JSON must have 'name' field");
        assertTrue(out.contains("\"parameters\""), "JSON must have 'parameters' field");
    }

    @Test
    void showNotFoundPatternExitsOneWithMessage() {
        int exit = commandLine.execute("--name", "notfound");
        assertEquals(1, exit);
        assertTrue(stderr.toString().contains("Pattern 'notfound' not found."));
    }
}
