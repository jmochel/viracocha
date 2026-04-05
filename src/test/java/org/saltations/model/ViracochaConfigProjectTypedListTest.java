package org.saltations.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies List&lt;ProjectEntry&gt; and nested MappingEntry round-trip through YAML.
 */
class ViracochaConfigProjectTypedListTest {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    @Test
    void projectWithMappingRoundTripsCorrectly() throws Exception {
        ViracochaConfig config = new ViracochaConfig();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("k1", "v1");
        MappingEntry mapping = new MappingEntry("pat-a", "out/dir", params);
        config.getProjects().add(new ProjectEntry("proj1", "/tmp/ws", List.of(mapping), new LinkedHashMap<>(), new java.util.ArrayList<>()));

        String serialized = yaml.writeValueAsString(config);
        ViracochaConfig deserialized = yaml.readValue(serialized, ViracochaConfig.class);

        assertEquals(1, deserialized.getProjects().size());
        ProjectEntry p = deserialized.getProjects().get(0);
        assertEquals("proj1", p.getName());
        assertEquals("/tmp/ws", p.getPath());
        assertEquals(1, p.getMappings().size());
        MappingEntry m = p.getMappings().get(0);
        assertEquals("pat-a", m.getPatternName());
        assertEquals("out/dir", m.getWorkspacePath());
        assertEquals("v1", m.getParameters().get("k1"));
    }

    @Test
    void emptyParametersMappingRoundTrips() throws Exception {
        ViracochaConfig config = new ViracochaConfig();
        config.getProjects().add(new ProjectEntry("p", "/ws", List.of(new MappingEntry("pat", "dest", new LinkedHashMap<>())), new LinkedHashMap<>(), new java.util.ArrayList<>()));
        String serialized = yaml.writeValueAsString(config);
        ViracochaConfig deserialized = yaml.readValue(serialized, ViracochaConfig.class);
        assertTrue(deserialized.getProjects().get(0).getMappings().get(0).getParameters().isEmpty());
    }

    @Test
    void projectParametersRoundTrip_andYamlWithoutParametersFieldLoadsEmpty() throws Exception {
        LinkedHashMap<String, String> projParams = new LinkedHashMap<>();
        projParams.put("env", "prod");
        ViracochaConfig config = new ViracochaConfig();
        config.getProjects().add(new ProjectEntry("proj1", "/tmp/ws", List.of(), projParams, new java.util.ArrayList<>()));

        String serialized = yaml.writeValueAsString(config);
        assertTrue(serialized.contains("parameters:"));
        ViracochaConfig deserialized = yaml.readValue(serialized, ViracochaConfig.class);
        assertEquals("prod", deserialized.getProjects().get(0).getParameters().get("env"));

        String legacy = """
            projects:
              - name: legacy
                path: /old/ws
                mappings: []
            """;
        ViracochaConfig fromLegacy = yaml.readValue(legacy, ViracochaConfig.class);
        assertTrue(fromLegacy.getProjects().get(0).getParameters().isEmpty());
    }
}
