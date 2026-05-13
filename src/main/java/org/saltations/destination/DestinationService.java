package org.saltations.destination;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.saltations.config.ConfigNotInitializedException;
import org.saltations.config.ConfigService;
import org.saltations.model.DestinationEntry;
import org.saltations.model.MappingEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for destination CRUD and mapping operations.
 * Commands are thin wrappers — all validation and persistence lives here.
 * D-04: destinations may not exist at registration time (no existence check).
 * D-08: load config fresh on each call; do NOT cache ViracochaConfig as a field.
 */
@Singleton
public class DestinationService {

    private final ConfigService configService;

    @Inject
    public DestinationService(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Validates and adds a named destination to config.
     * D-04/DEST-06: raw-string traversal check BEFORE any Path.of() call.
     * DEST-05: duplicate name check.
     * D-07: store path as-is (no normalization, no existence check).
     *
     * @throws IllegalArgumentException on validation failure (traversal or duplicate name)
     * @throws ConfigNotInitializedException if config has not been initialized
     * @throws IOException if config cannot be read or written
     */
    public DestinationEntry addDestination(String name, String rawPath) throws IOException {
        // DEST-06: raw string traversal check BEFORE Path.of() — normalization would hide ".."
        if (rawPath.contains("..")) {
            throw new IllegalArgumentException("Path must not contain '..': " + rawPath);
        }
        var config = configService.load();
        // DEST-05: duplicate name check
        var duplicate = config.getDestinations().stream()
            .anyMatch(d -> d.getName().equals(name));
        if (duplicate) {
            throw new IllegalArgumentException("Destination '" + name + "' already exists.");
        }
        // D-07: store path as-is (no normalization, no existence check per D-04)
        var entry = new DestinationEntry(name, rawPath,
            new LinkedHashMap<>(), new ArrayList<>());
        config.getDestinations().add(entry);
        configService.save(config);
        return entry;
    }

    /**
     * Returns all registered destinations. Empty list if none registered.
     *
     * @throws ConfigNotInitializedException if config has not been initialized
     * @throws IOException if config cannot be read
     */
    public List<DestinationEntry> listDestinations() throws IOException {
        return configService.load().getDestinations();
    }

    /**
     * Returns the destination with the given name, or Optional.empty() if not found.
     *
     * @throws ConfigNotInitializedException if config has not been initialized
     * @throws IOException if config cannot be read
     */
    public Optional<DestinationEntry> getDestination(String name) throws IOException {
        return configService.load().getDestinations().stream()
            .filter(d -> d.getName().equals(name))
            .findFirst();
    }

    /**
     * Removes the destination with the given name. Returns true if found and removed, false if not found.
     * D-16: callers print "Destination '<name>' not found." when this returns false.
     *
     * @throws ConfigNotInitializedException if config has not been initialized
     * @throws IOException if config cannot be read or written
     */
    public boolean removeDestination(String name) throws IOException {
        var config = configService.load();
        var removed = config.getDestinations().removeIf(d -> d.getName().equals(name));
        if (removed) {
            configService.save(config);
        }
        return removed;
    }

    /**
     * Adds a mapping from {@code sourceRef} to {@code destName}.
     * D-18: validates sourceRef exists in config.sources.
     * D-17: validates destination exists.
     *
     * @throws IllegalArgumentException if source or destination not found
     * @throws ConfigNotInitializedException if config has not been initialized
     * @throws IOException if config cannot be read or written
     */
    public void addMapping(String destName, String sourceRef, String glob,
                           boolean recurse, boolean sync) throws IOException {
        var config = configService.load();
        // MAP-04 / D-18: validate sourceRef exists in config.sources
        var sourceExists = config.getSources().stream()
            .anyMatch(s -> s.getName().equals(sourceRef));
        if (!sourceExists) {
            throw new IllegalArgumentException("Source '" + sourceRef + "' not found.");
        }
        // D-17: validate destination exists
        var dest = config.getDestinations().stream()
            .filter(d -> d.getName().equals(destName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Destination '" + destName + "' not found."));
        // MappingEntry field order: sourceRef, glob, recurse, sync, params
        dest.getMappings().add(new MappingEntry(sourceRef, glob, recurse, sync,
            new LinkedHashMap<>()));
        configService.save(config);
    }

    /**
     * Returns all mappings for the named destination. Returns empty list if destination not found.
     *
     * @throws ConfigNotInitializedException if config has not been initialized
     * @throws IOException if config cannot be read
     */
    public List<MappingEntry> listMappings(String destName) throws IOException {
        return configService.load().getDestinations().stream()
            .filter(d -> d.getName().equals(destName))
            .findFirst()
            .map(DestinationEntry::getMappings)
            .orElse(new ArrayList<>());
    }

    /**
     * Removes the mapping at the given 0-based index from the named destination.
     * Returns false if destination not found. Throws IndexOutOfBoundsException if index is invalid.
     *
     * @throws IndexOutOfBoundsException if index is negative or >= number of mappings
     * @throws ConfigNotInitializedException if config has not been initialized
     * @throws IOException if config cannot be read or written
     */
    public boolean removeMapping(String destName, int index) throws IOException {
        var config = configService.load();
        Optional<DestinationEntry> opt = config.getDestinations().stream()
            .filter(d -> d.getName().equals(destName))
            .findFirst();
        if (opt.isEmpty()) {
            return false; // caller prints D-27 error: "Destination '<name>' not found."
        }
        var dest = opt.get();
        if (index < 0 || index >= dest.getMappings().size()) {
            throw new IndexOutOfBoundsException(
                "Mapping index " + index + " out of range (destination has "
                + dest.getMappings().size() + " mappings).");
        }
        dest.getMappings().remove(index);
        configService.save(config);
        return true;
    }
}
