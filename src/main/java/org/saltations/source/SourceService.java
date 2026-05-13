package org.saltations.source;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.model.SourceEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for source CRUD operations.
 * Commands are thin wrappers — all validation and persistence lives here.
 * D-08: load config fresh on each call; do NOT cache ViracochaConfig as a field.
 */
@Singleton
public class SourceService {

    private final ConfigService configService;
    private final FreemarkerVariableExtractor extractor;

    @Inject
    public SourceService(ConfigService configService, FreemarkerVariableExtractor extractor) {
        this.configService = configService;
        this.extractor = extractor;
    }

    /**
     * Validates path and adds a named source to config.
     * D-01: validate path BEFORE resolving (traversal check on raw string).
     * D-08: method signatures per locked decisions.
     *
     * @throws IllegalArgumentException on validation failure (traversal, missing, not-dir, duplicate)
     * @throws ConfigNotInitializedException if config has not been initialized
     * @throws IOException if extractor fails or config cannot be read/written
     */
    public SourceEntry addSource(String name, String rawPath, boolean templates) throws IOException {
        // D-01.1 / SRC-06 / D-13: check raw string BEFORE Path resolution (normalization eats "..")
        if (rawPath.contains("..")) {
            throw new IllegalArgumentException("Path must not contain '..': " + rawPath);
        }
        // D-01.2 / D-14: resolve then check existence
        var p = Path.of(rawPath).toAbsolutePath().normalize();
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("Path does not exist: " + rawPath);
        }
        // D-01.3 / D-15: must be a directory
        if (!Files.isDirectory(p)) {
            throw new IllegalArgumentException("Path is not a directory: " + rawPath);
        }
        // SRC-05 / D-12: duplicate name check (name, not path — two names may share a path)
        var config = configService.load();
        var duplicate = config.getSources().stream()
            .anyMatch(s -> s.getName().equals(name));
        if (duplicate) {
            throw new IllegalArgumentException("Source '" + name + "' already exists.");
        }
        // SRC-07: extract Freemarker variables if templates=true
        List<String> params = templates
            ? extractor.extractFromDirectory(p)
            : new ArrayList<>();
        // D-01.4: store absolute, normalized path as string
        var entry = new SourceEntry(name, p.toString(), templates, params);
        config.getSources().add(entry);
        configService.save(config);
        return entry;
    }

    /**
     * Returns all registered sources. Empty list if none registered.
     *
     * @throws ConfigNotInitializedException if config has not been initialized
     * @throws IOException if config cannot be read
     */
    public List<SourceEntry> listSources() throws IOException {
        return configService.load().getSources();
    }

    /**
     * Returns the source with the given name, or Optional.empty() if not found.
     *
     * @throws ConfigNotInitializedException if config has not been initialized
     * @throws IOException if config cannot be read
     */
    public Optional<SourceEntry> getSource(String name) throws IOException {
        return configService.load().getSources().stream()
            .filter(s -> s.getName().equals(name))
            .findFirst();
    }

    /**
     * Removes the source with the given name. Returns true if found and removed, false if not found.
     * D-16: callers print "Source '<name>' not found." when this returns false.
     *
     * @throws ConfigNotInitializedException if config has not been initialized
     * @throws IOException if config cannot be read or written
     */
    public boolean removeSource(String name) throws IOException {
        var config = configService.load();
        var removed = config.getSources().removeIf(s -> s.getName().equals(name));
        if (removed) {
            configService.save(config);
        }
        return removed;
    }
}
