package org.saltations.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.destination.DestinationService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.infra.XdgPaths;
import org.saltations.source.SourceService;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stub tests for SyncCommand — all disabled as stubs.
 * Wave 3 (Plan 03) enables each test with real assertions.
 * Covers SYN-02 conflict exit code and SYN-03 through SYN-07.
 */
class SyncCommandTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private SourceService sourceService;
    private DestinationService destinationService;
    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;

    @BeforeEach
    void setUp() throws Exception {
        XdgPaths xdgPaths = new XdgPaths() {
            @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
        };
        configService = new ConfigService(xdgPaths);
        configService.init();
        FreemarkerVariableExtractor extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
        destinationService = new DestinationService(configService);
        DefaultSyncService syncServiceImpl = new DefaultSyncService(configService);
        SyncCommand cmd = new SyncCommand(syncServiceImpl);
        commandLine = new CommandLine(cmd);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    // --- SYN-03: sync command requires destination name ---

    @Disabled
    @Test
    void syncCommandRequiresDestinationName() {
    }

    // --- SYN-03: sync command with destination name routes ---

    @Disabled
    @Test
    void syncCommandWithDestinationNameRoutes() throws Exception {
    }

    // --- SYN-04: sync command dry-run reports without writing ---

    @Disabled
    @Test
    void syncCommandDryRunReportsWithoutWriting() throws Exception {
    }

    // --- SYN-05: sync command verbose prints per-file lines ---

    @Disabled
    @Test
    void syncCommandVerbosePrintsPerFileLines() throws Exception {
    }

    // --- SYN-06: sync command json outputs machine-readable ---

    @Disabled
    @Test
    void syncCommandJsonOutputsMachineReadable() throws Exception {
    }

    // --- SYN-07: sync command summary line always printed ---

    @Disabled
    @Test
    void syncCommandSummaryLineAlwaysPrinted() throws Exception {
    }

    // --- SYN-02: sync command returns exit 1 on conflict ---

    @Disabled
    @Test
    void syncCommandReturnsExitOneOnConflict() throws Exception {
    }
}
