package org.saltations.sync;

import org.junit.jupiter.api.BeforeEach;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Command-level integration tests for SyncCommand — Wave 3 (fully enabled).
 * Uses picocli test harness (CommandLine.execute) with captured stdout/stderr.
 * Plain JUnit 5, no @MicronautTest, same XdgPaths stub pattern as GenerateCommandTest.
 * Covers SYN-02 conflict exit code and SYN-03 through SYN-07.
 *
 * Note: CommandLine is rooted at SyncCommand directly (not as a subcommand of ViracochaCommand),
 * so execute() calls do NOT include "sync" as the first argument.
 */
class SyncCommandTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private SourceService sourceService;
    private DestinationService destinationService;
    private DefaultSyncService syncService;
    private SyncCommand cmd;
    private CommandLine commandLine;
    private ByteArrayOutputStream stdout;
    private ByteArrayOutputStream stderr;

    @BeforeEach
    void setUp() throws Exception {
        var xdgPaths = new XdgPaths(tempDir.toAbsolutePath().toString());
        configService = new ConfigService(xdgPaths);
        configService.init();
        var extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
        destinationService = new DestinationService(configService);
        syncService = new DefaultSyncService(configService);
        cmd = new SyncCommand(syncService);
        commandLine = new CommandLine(cmd);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    // --- SYN-03: sync command requires destination name (D-11, exit code 2) ---

    @Test
    void syncCommandRequiresDestinationName() {
        var exitCode = commandLine.execute();
        assertEquals(2, exitCode, "Exit code must be 2 when --dest is missing");
        assertTrue(stderr.toString().contains("Missing required option: '--dest'"),
            "Stderr must contain missing option message. Was: " + stderr.toString());
    }

    // --- SYN-03: sync command with destination name routes ---

    @Test
    void syncCommandWithDestinationNameRoutes() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-route"));
        Files.writeString(sourceDir.resolve("hello.txt"), "hello");
        Path destDir = Files.createDirectory(tempDir.resolve("dest-route"));
        sourceService.addSource("route-src", sourceDir.toString(), false);
        destinationService.addDestination("route-dest", destDir.toString());
        destinationService.addMapping("route-dest", "route-src", null, false, true); // sync=true

        var exitCode = commandLine.execute("--dest", "route-dest");

        assertEquals(0, exitCode, "Exit code must be 0 on success");
        assertTrue(stdout.toString().contains("Copied: "),
            "Stdout must contain 'Copied: '. Was: " + stdout.toString());
        assertTrue(Files.exists(destDir.resolve("hello.txt")),
            "hello.txt must be written to destination");
    }

    // --- SYN-04: sync command dry-run reports without writing ---

    @Test
    void syncCommandDryRunReportsWithoutWriting() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-dry"));
        Files.writeString(sourceDir.resolve("dry.txt"), "dry content");
        Path destDir = Files.createDirectory(tempDir.resolve("dest-dry"));
        sourceService.addSource("dry-src", sourceDir.toString(), false);
        destinationService.addDestination("dry-dest", destDir.toString());
        destinationService.addMapping("dry-dest", "dry-src", null, false, true);

        var exitCode = commandLine.execute("--dest", "dry-dest", "--dry-run");

        assertEquals(0, exitCode);
        assertFalse(Files.exists(destDir.resolve("dry.txt")),
            "File must NOT be written in dry-run mode");
        assertTrue(stdout.toString().contains("Copied: "),
            "Stdout must contain summary line. Was: " + stdout.toString());
    }

    // --- SYN-05: sync command verbose prints per-file lines (D-14) ---

    @Test
    void syncCommandVerbosePrintsPerFileLines() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-verbose"));
        Files.writeString(sourceDir.resolve("verbose.txt"), "verbose content");
        Path destDir = Files.createDirectory(tempDir.resolve("dest-verbose"));
        sourceService.addSource("verbose-src", sourceDir.toString(), false);
        destinationService.addDestination("verbose-dest", destDir.toString());
        destinationService.addMapping("verbose-dest", "verbose-src", null, false, true);

        var exitCode = commandLine.execute("--dest", "verbose-dest", "--verbose");

        assertEquals(0, exitCode);
        var out = stdout.toString();
        assertTrue(out.contains("Copied "),
            "Stdout must contain per-file 'Copied ' line. Was: " + out);
        assertTrue(out.contains("Copied: "),
            "Stdout must contain summary 'Copied: ' line. Was: " + out);
    }

    // --- SYN-07: sync command summary line always printed (D-13) ---

    @Test
    void syncCommandSummaryLineAlwaysPrinted() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-summary"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest-summary"));
        sourceService.addSource("summary-src", sourceDir.toString(), false);
        destinationService.addDestination("summary-dest", destDir.toString());
        destinationService.addMapping("summary-dest", "summary-src", null, false, true);

        var exitCode = commandLine.execute("--dest", "summary-dest");

        assertEquals(0, exitCode, "Exit code must be 0 when summary is printed");
        var out = stdout.toString();
        assertTrue(out.contains("Copied: "),    "Must contain 'Copied: '. Was: " + out);
        assertTrue(out.contains("Skipped: "),   "Must contain 'Skipped: '. Was: " + out);
        assertTrue(out.contains("Failed: "),    "Must contain 'Failed: '. Was: " + out);
        assertTrue(out.contains("Conflicts: "), "Must contain 'Conflicts: '. Was: " + out);
    }

    // --- SYN-02: sync command returns exit 1 on conflict (D-12) ---

    @Test
    void syncCommandReturnsExitOneOnConflict() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-conflict"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest-conflict"));
        Files.writeString(sourceDir.resolve("conflict.txt"), "source version");
        Files.writeString(destDir.resolve("conflict.txt"), "local changes");
        // Make dest newer so conflict is detected (D-01: dest.mtime > src.mtime)
        Files.setLastModifiedTime(destDir.resolve("conflict.txt"),
            FileTime.from(Instant.now().plusSeconds(60)));

        sourceService.addSource("conflict-src", sourceDir.toString(), false);
        destinationService.addDestination("conflict-dest", destDir.toString());
        destinationService.addMapping("conflict-dest", "conflict-src", null, false, true);

        var exitCode = commandLine.execute("--dest", "conflict-dest");

        assertEquals(1, exitCode,
            "Exit code must be 1 when conflict is detected. Stdout was: " + stdout.toString());
    }
}
