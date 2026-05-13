package org.saltations;

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
        try (var ctx = ApplicationContext.builder(
                ViracochaCommand.class, Environment.CLI).start()) {
            var cmd = new CommandLine(
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
            propagateUsageSettings(cmd);

            System.exit(cmd.execute(args));
        }
    }
 
    private static void propagateSeparator(CommandLine cmd, String separator) {
        cmd.setSeparator(separator);
        cmd.getSubcommands().values().forEach(sub -> propagateSeparator(sub, separator));
    }


    private static void propagateUsageSettings(CommandLine cmd) {

        var spec = cmd.getCommandSpec();

        spec.usageMessage().headerHeading("@|bold,underline Usage|@:%n%n");
        spec.usageMessage().descriptionHeading("%n@|bold,underline Description|@:%n");
        spec.usageMessage().sortOptions(false);

        boolean hasOptions = !spec.options().isEmpty();

        if (hasOptions) {
            spec.usageMessage().optionListHeading("%n@|bold,underline Options|@:%n");
        } else {
            spec.usageMessage().optionListHeading(""); // suppress header entirely
        }

        cmd.getSubcommands().values().forEach(sub -> propagateUsageSettings(sub));
    }


    @Override
    public Integer call() {
        return 0;
    }
}
