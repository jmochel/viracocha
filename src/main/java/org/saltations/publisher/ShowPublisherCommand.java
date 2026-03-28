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
 * Command: vira publisher show
 * Displays details of a specific publisher as a key-value block or JSON object.
 * Per D-02: Name: foo / Path: /some/path
 * Per D-03: --json outputs a single JSON object
 * Exit codes: 0 = success, 1 = not found or any error
 */
@Command(name = "show", description = "Show details of a registered publisher.", mixinStandardHelpOptions = true)
@Singleton
public class ShowPublisherCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--name"}, required = true, description = "Publisher name")
    private String name;

    @Option(names = {"--json"}, description = "Output as JSON")
    private boolean json;

    private final ConfigService configService;

    @Inject
    public ShowPublisherCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var config = configService.load();
            PublisherEntry entry = config.getPublishers().stream()
                .filter(e -> e.getName().equals(name))
                .findFirst()
                .orElse(null);
            if (entry == null) {
                spec.commandLine().getErr().println("Publisher '" + name + "' not found.");
                return 1;
            }
            if (json) {
                spec.commandLine().getOut().println(new ObjectMapper().writeValueAsString(entry));
            } else {
                spec.commandLine().getOut().println("Name: " + entry.getName());
                spec.commandLine().getOut().println("Path: " + entry.getPath());
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
