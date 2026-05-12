package org.saltations;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigCommand;
import org.saltations.destination.DestinationCommand;
import org.saltations.generate.GenerateCommand;
import org.saltations.source.SourceCommand;
import org.saltations.sync.SyncCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Root viracocha CLI command.
 * Entry point: main() wires System.exit() to the picocli execute() return value
 * so exit codes from Callable&lt;Integer&gt; are propagated to the shell.
 */
@Command(
    name = "vira",
    description = {
        "Workspace manager for AI-assisted development."
    },
    separator = " ",
    mixinStandardHelpOptions = true,
    subcommands = {
        ConfigCommand.class,
        SourceCommand.class,
        DestinationCommand.class,
        GenerateCommand.class,
        SyncCommand.class
    }
)
@Singleton
public class ViracochaCommand implements Callable<Integer> {

    public static void main(String[] args) throws Exception {
        try (ApplicationContext ctx = ApplicationContext.builder(
                ViracochaCommand.class, Environment.CLI).start()) {
            CommandLine cmd = new CommandLine(
                    ctx.getBean(ViracochaCommand.class),
                    new CommandLine.IFactory() {
                        @Override
                        public <K> K create(Class<K> cls) throws Exception {
                            return ctx.getBean(cls);
                        }
                    });
            cmd.setCommandName("vira");
            cmd.setSeparator(" ");
            cmd.getSubcommands().values().forEach(sub -> propagateSeparator(sub, " "));
            System.exit(cmd.execute(args));
        }
    }

    private static void propagateSeparator(CommandLine cmd, String separator) {
        cmd.setSeparator(separator);
        cmd.getSubcommands().values().forEach(sub -> propagateSeparator(sub, separator));
    }

    @Override
    public Integer call() {
        return 0;
    }
}
