package org.saltations.subscription;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Group command: vira subscription
 */
@Command(
    name = "subscription",
    description = "Manage publisher↔workspace subscriptions.",
    mixinStandardHelpOptions = true,
    subcommands = {
        AddSubscriptionCommand.class,
        ListSubscriptionsCommand.class,
        ShowSubscriptionCommand.class,
        RemoveSubscriptionCommand.class
    })
@Singleton
public class SubscriptionCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }
}
