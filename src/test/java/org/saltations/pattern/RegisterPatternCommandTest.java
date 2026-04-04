package org.saltations.pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.XdgPaths;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RegisterPatternCommandTest {

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
        RegisterPatternCommand command = new RegisterPatternCommand(configService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void registerWithValidPatternDirExtractsAndStoresVariables() throws Exception {
        Path patDir = Files.createDirectory(tempDir.resolve("mypat"));
        Files.writeString(patDir.resolve("template.txt"),
            "Hello ${name}, email: ${email}", StandardCharsets.UTF_8);

        int exit = commandLine.execute("--name", "mypat", "--path", patDir.toString());
        assertEquals(0, exit);
        assertTrue(stdout.toString().contains("Pattern 'mypat' registered."));
        ViracochaConfig config = configService.load();
        assertEquals(1, config.getPatterns().size());
        assertEquals("mypat", config.getPatterns().get(0).getName());
        assertTrue(config.getPatterns().get(0).getParameters().contains("name"));
        assertTrue(config.getPatterns().get(0).getParameters().contains("email"));
    }

    @Test
    void registerWithNonExistentPathExitsOneAndPrintsError() {
        int exit = commandLine.execute("--name", "bad", "--path", "/does/not/exist/ever");
        assertEquals(1, exit);
        assertTrue(stderr.toString().contains("Error: path does not exist:"));
    }

    @Test
    void registerDuplicateNameExitsOneAndLeavesConfigUnchanged() throws Exception {
        Path patDir = Files.createDirectory(tempDir.resolve("mypat"));
        commandLine.execute("--name", "mypat", "--path", patDir.toString());
        stdout.reset(); stderr.reset();
        int exit = commandLine.execute("--name", "mypat", "--path", patDir.toString());
        assertEquals(1, exit);
        assertTrue(stderr.toString().contains("already registered. Use unregister first."));
        assertEquals(1, configService.load().getPatterns().size());
    }

    @Test
    void registerPatternWithMalformedExpressionExitsOneAndDoesNotModifyConfig() throws Exception {
        Path patDir = Files.createDirectory(tempDir.resolve("broken"));
        Files.writeString(patDir.resolve("broken.txt"), "This is ${unclosed", StandardCharsets.UTF_8);
        int exit = commandLine.execute("--name", "broken", "--path", patDir.toString());
        assertEquals(1, exit);
        assertTrue(stderr.toString().contains("Malformed Freemarker expression"),
            "Error message must mention malformed expression");
        assertEquals(0, configService.load().getPatterns().size(), "Config must be unchanged");
    }

    @Test
    void registerWhenConfigNotInitializedExitsOne() {
        XdgPaths uninitXdg = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("uninit/config.yaml"); }
            @Override public Path configDir() { return tempDir.resolve("uninit"); }
            @Override public Path dataDir() { return tempDir.resolve("uninit/share"); }
        };
        RegisterPatternCommand cmd = new RegisterPatternCommand(new ConfigService(uninitXdg));
        CommandLine cl = new CommandLine(cmd);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        cl.setErr(new PrintWriter(err, true));
        int exit = cl.execute("--name", "x", "--path", "/tmp");
        assertEquals(1, exit);
        assertTrue(err.toString().contains("Config not initialized"));
    }
}
