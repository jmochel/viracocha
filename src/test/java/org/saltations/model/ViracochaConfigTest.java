package org.saltations.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for ViracochaConfig YAML round-trip.
 */
class ViracochaConfigTest {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    @Test
    void defaultConfigSerializesToYaml() throws Exception {
        ViracochaConfig config = new ViracochaConfig();
        String serialized = yaml.writeValueAsString(config);
        assertTrue(serialized.contains("version: 1"), "Serialized YAML must contain 'version: 1'");
        assertTrue(serialized.contains("publishers"), "Serialized YAML must contain 'publishers'");
        assertTrue(serialized.contains("patterns"), "Serialized YAML must contain 'patterns'");
        assertTrue(serialized.contains("projects"), "Serialized YAML must contain 'projects'");
    }

    @Test
    void configRoundTripPreservesVersion() throws Exception {
        ViracochaConfig original = new ViracochaConfig();
        String yaml_str = yaml.writeValueAsString(original);
        ViracochaConfig deserialized = yaml.readValue(yaml_str, ViracochaConfig.class);
        assertEquals(original.getVersion(), deserialized.getVersion(),
            "Round-trip must preserve version field");
    }
}
