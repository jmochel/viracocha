package org.saltations.catalog;

import jakarta.inject.Singleton;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Group command: vira catalog
 * Provides subcommands for managing registered catalogs.
 */
@Command(
    name = "catalog",
    aliases = {"cat"},
    description = "Manage registered catalogs.",
    mixinStandardHelpOptions = true,
    subcommands = {
        RegisterCatalogCommand.class,
        ListCatalogsCommand.class,
        ShowCatalogCommand.class,
        UnregisterCatalogCommand.class
    }
)
@Singleton
public class CatalogCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }
}
