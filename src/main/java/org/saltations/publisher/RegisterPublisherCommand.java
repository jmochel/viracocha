package org.saltations.publisher;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "register", description = "Register a named publisher.", mixinStandardHelpOptions = true)
@Singleton
public class RegisterPublisherCommand implements Callable<Integer> {
    @Override public Integer call() { return 0; }
}
