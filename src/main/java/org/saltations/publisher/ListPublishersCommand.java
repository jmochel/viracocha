package org.saltations.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.PublisherEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira publisher list
 * Lists all registered publishers in plain text (aligned columns) or JSONL (--json flag).
 * Per D-01: name  path (no headers, aligned columns)
 * Per D-03: --json outputs one JSON object per line
 * Exit codes: 0 = success, 1 = any error
 */
@Command(name = "list", description = "List all registered publishers.", mixinStandardHelpOptions = true)
@Singleton
public class ListPublishersCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--json"}, description = "Output as JSON (one object per line)")
    private boolean json;

    private final ConfigService configService;

    @Inject
    public ListPublishersCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var config = configService.load();
            if (json) {
                ObjectMapper om = new ObjectMapper();
                for (PublisherEntry e : config.getPublishers()) {
                    spec.commandLine().getOut().println(om.writeValueAsString(e));
                }
            } else {
                for (PublisherEntry e : config.getPublishers()) {
                    spec.commandLine().getOut().printf("%-20s  %s%n", e.getName(), e.getPath());
                }
            }
            return 0;
        } catch (ConfigNotInitializedException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        } catch (IOException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 1;
        }
    }
}
