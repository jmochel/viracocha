package org.saltations.pattern;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "show", description = "Show details of a registered pattern.", mixinStandardHelpOptions = true)
@Singleton
public class ShowPatternCommand implements Callable<Integer> {
    @Override public Integer call() { return 0; }
}
