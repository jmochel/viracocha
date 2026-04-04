package org.saltations.pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.PatternEntry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command: vira pattern list
 * Per D-01: name  path  param-count (aligned columns, no headers)
 * Per D-03: --json outputs one JSON object per line (includes parameters array)
 * Exit codes: 0 = success, 1 = any error
 */
@Command(name = "list", description = "List all registered patterns.", mixinStandardHelpOptions = true)
@Singleton
public class ListPatternsCommand implements Callable<Integer> {

    @Spec CommandSpec spec;

    @Option(names = {"--json"}, description = "Output as JSON (one object per line)")
    private boolean json;

    private final ConfigService configService;

    @Inject
    public ListPatternsCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        try {
            var config = configService.load();
            if (json) {
                ObjectMapper om = new ObjectMapper();
                for (PatternEntry e : config.getPatterns()) {
                    spec.commandLine().getOut().println(om.writeValueAsString(e));
                }
            } else {
                for (PatternEntry e : config.getPatterns()) {
                    spec.commandLine().getOut().printf("%-20s  %-40s  %d%n",
                        e.getName(), e.getPath(), e.getParameters().size());
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
