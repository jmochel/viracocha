package org.saltations.catalog;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira catalog unregister
 * Removes a named catalog from central config.
 */
@Command(name = "unregister", description = "Unregister a named catalog.", mixinStandardHelpOptions = true)
@Singleton
public class UnregisterCatalogCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"-n", "--name"}, required = true, description = "Catalog name to remove")
    private String name;

    private final ConfigService configService;

    @Inject
    public UnregisterCatalogCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var config = configService.load();
            boolean removed = config.getCatalogs().removeIf(e -> e.getName().equals(name));
            if (!removed) {
                spec.commandLine().getErr().println("Catalog '" + name + "' not found.");
                return 1;
            }
            configService.save(config);
            spec.commandLine().getOut().println("Catalog '" + name + "' unregistered.");
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
