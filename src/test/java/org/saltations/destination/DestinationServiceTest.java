package org.saltations.destination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.saltations.config.ConfigService;
import org.saltations.infra.FreemarkerVariableExtractor;
import org.saltations.infra.XdgPaths;
import org.saltations.model.DestinationEntry;
import org.saltations.source.SourceService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DestinationService. Uses inline XdgPaths stub and @TempDir isolation.
 * No @MicronautTest — plain JUnit 5 (mirrors SourceServiceTest pattern).
 * Covers DEST-01, DEST-05, DEST-06, MAP-05.
 */
class DestinationServiceTest {

    @TempDir
    Path tempDir;

    private DestinationService destinationService;
    private ConfigService configService;
    private SourceService sourceService;

    @BeforeEach
    void setUp() throws Exception {
        var xdgPaths = new XdgPaths("") {
            @Override public Path configFile() { return tempDir.resolve("viracocha").resolve("config.yaml"); }
            @Override public Path configDir()  { return tempDir.resolve("viracocha"); }
            @Override public Path dataDir()    { return tempDir.resolve("share").resolve("viracocha"); }
        };
        configService = new ConfigService(xdgPaths);
        configService.init();
        var extractor = new FreemarkerVariableExtractor();
        sourceService = new SourceService(configService, extractor);
        destinationService = new DestinationService(configService);
    }

    // --- DEST-06: path traversal rejection ---

    @Test
    void addDestinationRejectsPathWithDotDot() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> destinationService.addDestination("ws", "/home/user/../etc"));
        assertEquals("Path must not contain '..': /home/user/../etc", ex.getMessage());
    }

    // --- DEST-05: duplicate name rejection ---

    @Test
    void addDestinationRejectsDuplicateName() throws Exception {
        destinationService.addDestination("my-dest", "/home/user/workspace");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> destinationService.addDestination("my-dest", "/home/user/other"));
        assertEquals("Destination 'my-dest' already exists.", ex.getMessage());
    }

    // --- DEST-01: successful add returns correct entry ---

    @Test
    void addDestinationSuccessReturnsEntryWithCorrectFields() throws Exception {
        var result = destinationService.addDestination("my-ws", "/home/user/workspace");
        assertEquals("my-ws", result.getName());
        assertEquals("/home/user/workspace", result.getPath());
    }

    // --- DEST-01: path stored as-is (no normalization) ---

    @Test
    void addDestinationStoresPathAsIs() throws Exception {
        var result = destinationService.addDestination("tilde-ws", "~/workspace");
        assertEquals("~/workspace", result.getPath());
    }

    // --- listDestinations ---

    @Test
    void listDestinationsEmptyOnFreshConfig() throws Exception {
        assertTrue(destinationService.listDestinations().isEmpty());
    }

    @Test
    void listDestinationsReturnsAllAdded() throws Exception {
        destinationService.addDestination("alpha", "/tmp/alpha");
        destinationService.addDestination("beta", "/tmp/beta");
        List<DestinationEntry> all = destinationService.listDestinations();
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(d -> d.getName().equals("alpha")));
        assertTrue(all.stream().anyMatch(d -> d.getName().equals("beta")));
    }

    // --- getDestination ---

    @Test
    void getDestinationReturnsEmptyForUnknownName() throws Exception {
        assertEquals(Optional.empty(), destinationService.getDestination("ghost"));
    }

    @Test
    void getDestinationReturnsPresentForKnownName() throws Exception {
        destinationService.addDestination("known", "/tmp/known");
        var result = destinationService.getDestination("known");
        assertTrue(result.isPresent());
        assertEquals("known", result.get().getName());
    }

    // --- removeDestination ---

    @Test
    void removeDestinationReturnsFalseForUnknownName() throws Exception {
        assertFalse(destinationService.removeDestination("ghost"));
    }

    @Test
    void removeDestinationReturnsTrueAndRemovesEntry() throws Exception {
        destinationService.addDestination("removeme", "/tmp/removeme");
        assertTrue(destinationService.removeDestination("removeme"));
        assertEquals(Optional.empty(), destinationService.getDestination("removeme"));
    }

    // --- addMapping: source validation ---

    @Test
    void addMappingThrowsWhenSourceRefNotFound() throws Exception {
        destinationService.addDestination("dest1", "/tmp/dest1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> destinationService.addMapping("dest1", "nosuch-src", null, false, false));
        assertEquals("Source 'nosuch-src' not found.", ex.getMessage());
    }

    // --- addMapping: destination validation ---

    @Test
    void addMappingThrowsWhenDestinationNotFound() throws Exception {
        Path srcDir = Files.createDirectory(tempDir.resolve("src1"));
        sourceService.addSource("my-src", srcDir.toString(), false);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> destinationService.addMapping("no-dest", "my-src", null, false, false));
        assertEquals("Destination 'no-dest' not found.", ex.getMessage());
    }

    // --- addMapping: success ---

    @Test
    void addMappingSucceedsAndAppearsInListMappings() throws Exception {
        Path srcDir = Files.createDirectory(tempDir.resolve("src1"));
        sourceService.addSource("my-src", srcDir.toString(), false);
        destinationService.addDestination("my-dest", "/tmp/my-dest");
        destinationService.addMapping("my-dest", "my-src", "**/*.md", true, false);
        var mappings = destinationService.listMappings("my-dest");
        assertEquals(1, mappings.size());
        var m = mappings.get(0);
        assertEquals("my-src", m.getSourceRef());
        assertEquals("**/*.md", m.getGlob());
        assertTrue(m.isRecurse());
        assertFalse(m.isSync());
    }

    // --- removeMapping: destination not found ---

    @Test
    void removeMappingReturnsFalseWhenDestinationNotFound() throws Exception {
        assertFalse(destinationService.removeMapping("no-dest", 0));
    }

    // --- removeMapping: index out of range ---

    @Test
    void removeMappingThrowsIndexOutOfBoundsForOutOfRangeIndex() throws Exception {
        destinationService.addDestination("dest2", "/tmp/dest2");
        IndexOutOfBoundsException ex = assertThrows(IndexOutOfBoundsException.class,
            () -> destinationService.removeMapping("dest2", 0));
        assertEquals("Mapping index 0 out of range (destination has 0 mappings).", ex.getMessage());
    }

    // --- removeMapping: success ---

    @Test
    void removeMappingRemovesCorrectMappingByZeroBasedIndex() throws Exception {
        Path srcDir = Files.createDirectory(tempDir.resolve("src2"));
        sourceService.addSource("src2", srcDir.toString(), false);
        destinationService.addDestination("dest3", "/tmp/dest3");
        destinationService.addMapping("dest3", "src2", null, false, false);
        destinationService.addMapping("dest3", "src2", "*.md", false, false);
        assertTrue(destinationService.removeMapping("dest3", 0));
        var remaining = destinationService.listMappings("dest3");
        assertEquals(1, remaining.size());
        assertEquals("*.md", remaining.get(0).getGlob());
    }
}
