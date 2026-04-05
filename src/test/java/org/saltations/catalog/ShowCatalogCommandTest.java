package org.saltations.catalog;

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

class ShowCatalogCommandTest {

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
        ShowCatalogCommand command = new ShowCatalogCommand(configService);
        commandLine = new CommandLine(command);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    @Test
    void showExistingCatalogPrintsKeyValueBlock() throws Exception {
        Path pubDir = Files.createDirectory(tempDir.resolve("mypub"));
        new CommandLine(new RegisterCatalogCommand(configService))
            .execute("--name", "mypub", "--path", pubDir.toString());

        int exit = commandLine.execute("--name", "mypub");
        assertEquals(0, exit);
        String out = stdout.toString();
        assertTrue(out.contains("Name: mypub"), "Output must contain 'Name: mypub'");
        assertTrue(out.contains("Path: " + pubDir), "Output must contain 'Path: ' followed by path");
    }

    @Test
    void showWithJsonFlagOutputsSingleJsonObject() throws Exception {
        Path pubDir = Files.createDirectory(tempDir.resolve("mypub"));
        new CommandLine(new RegisterCatalogCommand(configService))
            .execute("--name", "mypub", "--path", pubDir.toString());

        int exit = commandLine.execute("--name", "mypub", "--json");
        assertEquals(0, exit);
        String out = stdout.toString();
        assertTrue(out.contains("\"name\""), "JSON output must contain 'name' field");
        assertTrue(out.contains("\"path\""), "JSON output must contain 'path' field");
        assertTrue(out.contains("mypub"));
    }

    @Test
    void showNotFoundNameExitsOneWithMessage() {
        int exit = commandLine.execute("--name", "notfound");
        assertEquals(1, exit);
        assertTrue(stderr.toString().contains("Catalog 'notfound' not found."));
    }

    @Test
    void showWhenConfigNotInitializedExitsOne() {
        XdgPaths uninitXdg = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("uninit/config.yaml"); }
            @Override public Path configDir() { return tempDir.resolve("uninit"); }
            @Override public Path dataDir() { return tempDir.resolve("uninit/share"); }
        };
        ShowCatalogCommand cmd = new ShowCatalogCommand(new ConfigService(uninitXdg));
        CommandLine cl = new CommandLine(cmd);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        cl.setErr(new PrintWriter(err, true));
        int exit = cl.execute("--name", "x");
        assertEquals(1, exit);
        assertTrue(err.toString().contains("Config not initialized"));
    }
}
