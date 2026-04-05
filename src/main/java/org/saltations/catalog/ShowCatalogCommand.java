package org.saltations.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.CatalogEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira catalog show
 * Displays details of a specific catalog as a key-value block or JSON object.
 */
@Command(name = "show", description = "Show details of a registered catalog.", mixinStandardHelpOptions = true)
@Singleton
public class ShowCatalogCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"-n", "--name"}, required = true, description = "Catalog name")
    private String name;

    @Option(names = {"--json"}, description = "Output as JSON")
    private boolean json;

    private final ConfigService configService;

    @Inject
    public ShowCatalogCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var config = configService.load();
            CatalogEntry entry = config.getCatalogs().stream()
                .filter(e -> e.getName().equals(name))
                .findFirst()
                .orElse(null);
            if (entry == null) {
                spec.commandLine().getErr().println("Catalog '" + name + "' not found.");
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
