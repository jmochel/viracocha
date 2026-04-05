package org.saltations;

import io.micronaut.configuration.picocli.PicocliRunner;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigCommand;
import org.saltations.pattern.PatternCommand;
import org.saltations.project.ProjectCommand;
import org.saltations.publisher.PublisherCommand;
import org.saltations.generate.GenerateCommand;
import org.saltations.subscription.SubscriptionCommand;
import org.saltations.sync.SyncCommand;
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
        "Workspace manager for AI-assisted development.", 
        "   - Syncs project's AI config artifacts FROM 'Publisher' folders",
        "   - Syncs project's AI config artifacts TO 'Publisher' folders (if desired)",
        "   - Generates folders/files FROM 'Pattern' folders/files into a project folder", 
        "   - Mapping between 'Patterns' and 'Publishers' is defined in a 'Project' file"
    },
    mixinStandardHelpOptions = true,
    subcommands = {
        ConfigCommand.class,
        PublisherCommand.class,
        PatternCommand.class,
        ProjectCommand.class,
        SubscriptionCommand.class,
        GenerateCommand.class,
        SyncCommand.class
    }
)
@Singleton
public class ViracochaCommand implements Callable<Integer> {

    public static void main(String[] args) throws Exception {
        System.exit(PicocliRunner.execute(ViracochaCommand.class, args));
    }

    @Override
    public Integer call() {
        return 0;
    }
}
