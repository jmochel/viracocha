package org.saltations.publisher;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "unregister", description = "Unregister a named publisher.", mixinStandardHelpOptions = true)
@Singleton
public class UnregisterPublisherCommand implements Callable<Integer> {
    @Override public Integer call() { return 0; }
}
