package org.saltations.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that typed List<PublisherEntry> and List<PatternEntry> round-trip
 * correctly through jackson-dataformat-yaml serialization.
 */
class ViracochaConfigTypedListTest {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    @Test
    void emptyListsSerializeToYamlWithoutError() throws Exception {
        ViracochaConfig config = new ViracochaConfig();
        String serialized = yaml.writeValueAsString(config);
        assertTrue(serialized.contains("publishers"), "YAML must contain publishers key");
        assertTrue(serialized.contains("patterns"), "YAML must contain patterns key");
    }

    @Test
    void publisherEntryRoundTripsCorrectly() throws Exception {
        ViracochaConfig config = new ViracochaConfig();
        config.getPublishers().add(new PublisherEntry("mypub", "/tmp/pub"));
        String serialized = yaml.writeValueAsString(config);
        ViracochaConfig deserialized = yaml.readValue(serialized, ViracochaConfig.class);
        assertEquals(1, deserialized.getPublishers().size());
        assertEquals("mypub", deserialized.getPublishers().get(0).getName());
        assertEquals("/tmp/pub", deserialized.getPublishers().get(0).getPath());
    }

    @Test
    void patternEntryWithParametersRoundTripsCorrectly() throws Exception {
        ViracochaConfig config = new ViracochaConfig();
        PatternEntry entry = new PatternEntry("mypat", "/tmp/pat", List.of("name", "email"));
        config.getPatterns().add(entry);
        String serialized = yaml.writeValueAsString(config);
        ViracochaConfig deserialized = yaml.readValue(serialized, ViracochaConfig.class);
        assertEquals(1, deserialized.getPatterns().size());
        assertEquals("mypat", deserialized.getPatterns().get(0).getName());
        assertEquals(List.of("name", "email"), deserialized.getPatterns().get(0).getParameters());
    }
}
