package org.saltations.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.PublisherEntry;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command: vira publisher register
 * Registers a named publisher (a local directory path) in central config.
 * Validates path exists. Rejects duplicates.
 * Exit codes: 0 = success, 1 = any error
 */
@Command(name = "register", description = "Register a named publisher.", mixinStandardHelpOptions = true)
@Singleton
public class RegisterPublisherCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"-n", "--name"}, required = true, description = "Publisher name")
    private String name;

    @Option(names = {"-p", "--path"}, required = true, description = "Absolute path to publisher directory")
    private String path;

    private final ConfigService configService;

    @Inject
    public RegisterPublisherCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            ViracochaConfig config = configService.load();

            // PUB-02: validate path exists
            if (!Files.exists(Path.of(path))) {
                spec.commandLine().getErr().println("Error: path does not exist: " + path);
                return 1;
            }

            // D-06: reject duplicate name
            boolean alreadyExists = config.getPublishers().stream()
                .anyMatch(e -> e.getName().equals(name));
            if (alreadyExists) {
                spec.commandLine().getErr().println(
                    "Publisher '" + name + "' already registered. Use unregister first.");
                return 1;
            }

            config.getPublishers().add(new PublisherEntry(name, path));
            configService.save(config);
            spec.commandLine().getOut().println("Publisher '" + name + "' registered.");
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
