package org.saltations.generate;

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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Command-level integration tests for GenerateCommand — Wave 0 test scaffold.
 * Uses picocli test harness (CommandLine.execute) with captured stdout/stderr.
 * Plain JUnit 5, no @MicronautTest, same XdgPaths stub pattern as GeneratorServiceTest.
 * Covers GEN-05 through GEN-07.
 */
class GenerateCommandTest {

    @TempDir
    Path tempDir;

    private ConfigService configService;
    private SourceService sourceService;
    private DestinationService destinationService;
    private PathExpander pathExpander;
    private GeneratorService generatorService;
    private GenerateCommand cmd;
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
        pathExpander = new PathExpander();
        generatorService = new GeneratorService(configService, pathExpander);
        cmd = new GenerateCommand(generatorService);
        commandLine = new CommandLine(cmd);
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(stdout, true));
        commandLine.setErr(new PrintWriter(stderr, true));
    }

    // --- GEN-05/D-14: missing --destination-name ---
    // This test is NOT @Disabled — the null check is already wired in GenerateCommand.call()

    @Test
    void generateCommandRequiresDestinationName() {
        // Execute without --destination-name; since we build CommandLine directly from GenerateCommand,
        // we call execute() with no subcommand routing (the command IS the root here)
        int exitCode = commandLine.execute();
        assertEquals(2, exitCode, "Exit code should be 2 when --destination-name is missing");
        assertTrue(stderr.toString().contains("Missing required option: '--destination-name'"),
            "Stderr should contain the missing option message");
    }

    // --- GEN-05: routing by destination name ---

    @Disabled("Wave 2: implement after Plan 01")
    @Test
    void generateCommandWithDestinationNameRoutes() throws Exception {
        // Set up source and destination
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-route"));
        Files.writeString(sourceDir.resolve("hello.txt"), "hello");
        sourceService.addSource("route-src", sourceDir.toString(), false);
        Path destDir = Files.createDirectory(tempDir.resolve("dest-route"));
        destinationService.addDestination("my-dest", destDir.toString());
        destinationService.addMapping("my-dest", "route-src", null, false, false);

        int exitCode = commandLine.execute("generate", "--destination-name", "my-dest");

        assertEquals(0, exitCode, "Exit code should be 0 on success");
        assertTrue(stdout.toString().contains("Generated:"), "Stdout should contain 'Generated:' summary line");
    }

    // --- GEN-06: dry-run mode ---

    @Disabled("Wave 2: implement after Plan 01")
    @Test
    void generateCommandDryRunReportsActionsWithoutWriting() throws Exception {
        // Set up source with one file
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-dryrun"));
        Files.writeString(sourceDir.resolve("dryrun.txt"), "dry content");
        sourceService.addSource("dryrun-src", sourceDir.toString(), false);
        Path destDir = Files.createDirectory(tempDir.resolve("dest-dryrun"));
        destinationService.addDestination("dryrun-dest", destDir.toString());
        destinationService.addMapping("dryrun-dest", "dryrun-src", null, false, false);

        int exitCode = commandLine.execute("generate", "--destination-name", "dryrun-dest", "--dry-run");

        assertEquals(0, exitCode, "Exit code should be 0 on dry-run");
        assertTrue(stdout.toString().contains("Would create"), "Stdout should report 'Would create' in dry-run mode");
        assertFalse(Files.exists(destDir.resolve("dryrun.txt")), "File must NOT be written in dry-run mode");
    }

    // --- GEN-07: verbose per-file lines ---

    @Disabled("Wave 2: implement after Plan 01")
    @Test
    void generateCommandVerbosePrintsPerFileLines() throws Exception {
        // Set up source with one file
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-verbose"));
        Files.writeString(sourceDir.resolve("verbose.txt"), "verbose content");
        sourceService.addSource("verbose-src", sourceDir.toString(), false);
        Path destDir = Files.createDirectory(tempDir.resolve("dest-verbose"));
        destinationService.addDestination("verbose-dest", destDir.toString());
        destinationService.addMapping("verbose-dest", "verbose-src", null, false, false);

        int exitCode = commandLine.execute("generate", "--destination-name", "verbose-dest", "--verbose");

        assertEquals(0, exitCode, "Exit code should be 0 on verbose run");
        assertTrue(stdout.toString().contains("Created "), "Stdout should contain 'Created ' per-file line in verbose mode");
    }

    // --- GEN-05/D-15: summary line always printed ---

    @Disabled("Wave 2: implement after Plan 01")
    @Test
    void generateCommandSummaryLineAlwaysPrinted() throws Exception {
        // Set up a registered destination with at least one mapping
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-summary"));
        Files.writeString(sourceDir.resolve("summary.txt"), "summary content");
        sourceService.addSource("summary-src", sourceDir.toString(), false);
        Path destDir = Files.createDirectory(tempDir.resolve("dest-summary"));
        destinationService.addDestination("summary-dest", destDir.toString());
        destinationService.addMapping("summary-dest", "summary-src", null, false, false);

        commandLine.execute("generate", "--destination-name", "summary-dest");

        String out = stdout.toString();
        assertTrue(out.contains("Generated: "), "Stdout should contain 'Generated: ' in summary");
        assertTrue(out.contains("Skipped: "), "Stdout should contain 'Skipped: ' in summary");
        assertTrue(out.contains("Failed: "), "Stdout should contain 'Failed: ' in summary");
    }

    // --- GEN-05: unknown destination exits 1 ---

    @Disabled("Wave 2: implement after Plan 01")
    @Test
    void generateCommandUnknownDestinationExitsOne() {
        int exitCode = commandLine.execute("generate", "--destination-name", "unknown-dest");

        assertEquals(1, exitCode, "Exit code should be 1 for unknown destination");
        assertTrue(stderr.toString().contains("unknown-dest"), "Stderr should contain the unknown destination name");
    }
}
