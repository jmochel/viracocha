package org.saltations.publisher;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "list", description = "List all registered publishers.", mixinStandardHelpOptions = true)
@Singleton
public class ListPublishersCommand implements Callable<Integer> {
    @Override public Integer call() { return 0; }
}
