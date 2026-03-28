package org.saltations.pattern;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "unregister", description = "Unregister a named pattern.", mixinStandardHelpOptions = true)
@Singleton
public class UnregisterPatternCommand implements Callable<Integer> {
    @Override public Integer call() { return 0; }
}
