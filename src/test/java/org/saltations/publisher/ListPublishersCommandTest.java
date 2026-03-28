package org.saltations.publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.XdgPaths;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ListPublishersCommandTest {

    @TempDir Path tempDir;
    private CommandLine listCommandLine;
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
        ListPublishersCommand command = new ListPublishersCommand(configService);
        listCommandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        listCommandLine.setOut(new PrintWriter(stdout, true));
        listCommandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void listWithTwoPublishersPrintsBothEntries() throws Exception {
        Path d1 = Files.createDirectory(tempDir.resolve("pub1"));
        Path d2 = Files.createDirectory(tempDir.resolve("pub2"));
        // Pre-populate config via register command
        RegisterPublisherCommand reg = new RegisterPublisherCommand(configService);
        CommandLine regCl = new CommandLine(reg);
        regCl.execute("--name", "alpha", "--path", d1.toString());
        regCl.execute("--name", "beta", "--path", d2.toString());

        int exit = listCommandLine.execute();
        assertEquals(0, exit);
        String out = stdout.toString();
        assertTrue(out.contains("alpha"));
        assertTrue(out.contains("beta"));
        assertTrue(out.contains(d1.toString()));
        assertTrue(out.contains(d2.toString()));
    }

    @Test
    void listWithJsonFlagOutputsJsonObjects() throws Exception {
        Path d1 = Files.createDirectory(tempDir.resolve("pub1"));
        RegisterPublisherCommand reg = new RegisterPublisherCommand(configService);
        new CommandLine(reg).execute("--name", "mypub", "--path", d1.toString());

        int exit = listCommandLine.execute("--json");
        assertEquals(0, exit);
        String out = stdout.toString();
        assertTrue(out.contains("\"name\""), "JSON output must contain 'name' field");
        assertTrue(out.contains("\"path\""), "JSON output must contain 'path' field");
        assertTrue(out.contains("mypub"));
    }

    @Test
    void listWhenEmptyExitsZeroWithNoOutput() {
        int exit = listCommandLine.execute();
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
        ListPublishersCommand cmd = new ListPublishersCommand(new ConfigService(uninitXdg));
        CommandLine cl = new CommandLine(cmd);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        cl.setErr(new PrintWriter(err, true));
        int exit = cl.execute();
        assertEquals(1, exit);
        assertTrue(err.toString().contains("Config not initialized"));
    }
}
