package org.saltations.pattern;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "list", description = "List all registered patterns.", mixinStandardHelpOptions = true)
@Singleton
public class ListPatternsCommand implements Callable<Integer> {
    @Override public Integer call() { return 0; }
}
