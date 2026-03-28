package org.saltations.publisher;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "show", description = "Show details of a registered publisher.", mixinStandardHelpOptions = true)
@Singleton
public class ShowPublisherCommand implements Callable<Integer> {
    @Override public Integer call() { return 0; }
}
