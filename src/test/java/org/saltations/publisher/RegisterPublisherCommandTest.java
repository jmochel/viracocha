package org.saltations.publisher;

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

class RegisterPublisherCommandTest {

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
        configService.init();  // initialize config so most tests have it ready
        RegisterPublisherCommand command = new RegisterPublisherCommand(configService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void registerWithExistingPathExitsZeroAndPersistsEntry() throws Exception {
        Path pubDir = Files.createDirectory(tempDir.resolve("mypub"));
        int exit = commandLine.execute("--name", "mypub", "--path", pubDir.toString());
        assertEquals(0, exit);
        assertTrue(stdout.toString().contains("Publisher 'mypub' registered."));
        ViracochaConfig config = configService.load();
        assertEquals(1, config.getPublishers().size());
        assertEquals("mypub", config.getPublishers().get(0).getName());
        assertEquals(pubDir.toString(), config.getPublishers().get(0).getPath());
    }

    @Test
    void registerWithNonExistentPathExitsOneAndPrintsError() throws Exception {
        int exit = commandLine.execute("--name", "bad", "--path", "/does/not/exist/ever");
        assertEquals(1, exit);
        assertTrue(stderr.toString().contains("Error: path does not exist:"));
        assertEquals(0, configService.load().getPublishers().size());
    }

    @Test
    void registerDuplicateNameExitsOneAndLeavesConfigUnchanged() throws Exception {
        Path pubDir = Files.createDirectory(tempDir.resolve("mypub"));
        commandLine.execute("--name", "mypub", "--path", pubDir.toString());
        stdout.reset(); stderr.reset();
        int exit = commandLine.execute("--name", "mypub", "--path", pubDir.toString());
        assertEquals(1, exit);
        assertTrue(stderr.toString().contains("already registered. Use unregister first."));
        assertEquals(1, configService.load().getPublishers().size());
    }

    @Test
    void registerWithoutInitializedConfigExitsOneWithMessage() {
        // Create a fresh command with an uninitialized config dir
        XdgPaths uninitXdg = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("uninit/config.yaml"); }
            @Override public Path configDir() { return tempDir.resolve("uninit"); }
            @Override public Path dataDir() { return tempDir.resolve("uninit/share"); }
        };
        ConfigService uninitService = new ConfigService(uninitXdg);
        RegisterPublisherCommand cmd = new RegisterPublisherCommand(uninitService);
        CommandLine cl = new CommandLine(cmd);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        cl.setErr(new PrintWriter(err, true));
        int exit = cl.execute("--name", "x", "--path", "/tmp");
        assertEquals(1, exit);
        assertTrue(err.toString().contains("Config not initialized"));
    }
}
