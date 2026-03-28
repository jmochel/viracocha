package org.saltations;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for ViracochaCommand root. Verifies the command hierarchy is wired
 * (--help recognizes 'config' subcommand) and exit codes work.
 */
public class ViracochaCommandTest {

    @Test
    public void helpExitsZeroAndListsConfigSubcommand() throws Exception {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            CommandLine commandLine = new CommandLine(
                ctx.getBean(ViracochaCommand.class),
                new io.micronaut.configuration.picocli.MicronautFactory(ctx));
            commandLine.setOut(new PrintWriter(stdout, true));
            int exitCode = commandLine.execute("--help");
            assertEquals(0, exitCode, "--help must exit 0");
            assertTrue(stdout.toString().contains("config"),
                "--help output must list 'config' subcommand");
        }
    }
}
