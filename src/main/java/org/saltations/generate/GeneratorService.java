package org.saltations.generate;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigService;

import java.io.IOException;

/**
 * Stub implementation — will be rewritten in Phase 11 for v3 source/destination model.
 */
@Singleton
public class GeneratorService {

    private final ConfigService configService;
    private final PathExpander pathExpander;

    @Inject
    public GeneratorService(ConfigService configService, PathExpander pathExpander) {
        this.configService = configService;
        this.pathExpander = pathExpander;
    }

    public GenerationResult generate(String projectName, boolean dryRun, boolean verbose) throws IOException {
        throw new UnsupportedOperationException(
            "GeneratorService not yet implemented for v3 — rewrite in Phase 11.");
    }
}
