package org.saltations.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for v3 ViracochaConfig YAML round-trip (CFG-01).
 */
class ViracochaConfigV3Test {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    @Test
    void defaultConfigSerializesToYamlWithV3Fields() throws Exception {
        ViracochaConfig config = new ViracochaConfig();
        String serialized = yaml.writeValueAsString(config);
        assertTrue(serialized.contains("version: 3"), "Serialized YAML must contain 'version: 3'");
        assertTrue(serialized.contains("sources"), "Serialized YAML must contain 'sources'");
        assertTrue(serialized.contains("destinations"), "Serialized YAML must contain 'destinations'");
        assertFalse(serialized.contains("catalogs"), "v3 YAML must NOT contain 'catalogs'");
        assertFalse(serialized.contains("archetypes"), "v3 YAML must NOT contain 'archetypes'");
        assertFalse(serialized.contains("projects"), "v3 YAML must NOT contain 'projects'");
    }

    @Test
    void configRoundTripPreservesVersion() throws Exception {
        ViracochaConfig original = new ViracochaConfig();
        String serialized = yaml.writeValueAsString(original);
        ViracochaConfig deserialized = yaml.readValue(serialized, ViracochaConfig.class);
        assertEquals(3, deserialized.getVersion(), "Round-trip must preserve version=3");
    }

    @Test
    void configRoundTripPreservesEmptyLists() throws Exception {
        ViracochaConfig original = new ViracochaConfig();
        String serialized = yaml.writeValueAsString(original);
        ViracochaConfig deserialized = yaml.readValue(serialized, ViracochaConfig.class);
        assertNotNull(deserialized.getSources(), "sources must not be null after round-trip");
        assertNotNull(deserialized.getDestinations(), "destinations must not be null after round-trip");
        assertTrue(deserialized.getSources().isEmpty(), "sources must be empty after round-trip");
        assertTrue(deserialized.getDestinations().isEmpty(), "destinations must be empty after round-trip");
    }
}
