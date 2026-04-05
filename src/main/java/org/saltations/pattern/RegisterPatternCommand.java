package org.saltations.pattern;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.PatternEntry;
import org.saltations.model.ViracochaConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command: vira pattern register
 * Registers a named pattern. Validates path exists. Extracts Freemarker variables.
 * Per PAT-02: validates path exists before registering.
 * Per PAT-03: extracts Freemarker variable names via FreemarkerVariableExtractor.
 * Per D-05: fails fast if any file contains a malformed expression.
 * Per D-06: rejects duplicate name.
 * Exit codes: 0 = success, 1 = any error
 */
@Command(name = "register", description = "Register a named pattern.", mixinStandardHelpOptions = true)
@Singleton
public class RegisterPatternCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"-n", "--name"}, required = true, description = "Pattern name")
    private String name;

    @Option(names = {"-p", "--path"}, required = true, description = "Absolute path to pattern directory")
    private String path;

    private final ConfigService configService;
    private final FreemarkerVariableExtractor extractor = new FreemarkerVariableExtractor();

    @Inject
    public RegisterPatternCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            ViracochaConfig config = configService.load();

            // PAT-02: validate path exists
            if (!Files.exists(Path.of(path))) {
                spec.commandLine().getErr().println("Error: path does not exist: " + path);
                return 1;
            }

            // D-06: reject duplicate name
            boolean alreadyExists = config.getPatterns().stream()
                .anyMatch(e -> e.getName().equals(name));
            if (alreadyExists) {
                spec.commandLine().getErr().println(
                    "Pattern '" + name + "' already registered. Use unregister first.");
                return 1;
            }

            // PAT-03: extract Freemarker variables (D-05: throws on malformed)
            List<String> parameters = extractor.extractFromDirectory(Path.of(path));

            config.getPatterns().add(new PatternEntry(name, path, parameters));
            configService.save(config);
            spec.commandLine().getOut().println("Pattern '" + name + "' registered.");
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
