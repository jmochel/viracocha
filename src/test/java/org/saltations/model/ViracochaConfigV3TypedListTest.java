package org.saltations.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip tests for v3 typed list fields: SourceEntry, DestinationEntry, MappingEntry (CFG-01).
 */
class ViracochaConfigV3TypedListTest {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    @Test
    void sourceEntryRoundTrip() throws Exception {
        var src = new SourceEntry("s1", "/tmp/s", true, List.of("foo", "bar"));
        var cfg = new ViracochaConfig();
        cfg.getSources().add(src);
        var serialized = yaml.writeValueAsString(cfg);
        var deserialized = yaml.readValue(serialized, ViracochaConfig.class);
        assertEquals(1, deserialized.getSources().size());
        var got = deserialized.getSources().get(0);
        assertEquals("s1", got.getName());
        assertEquals("/tmp/s", got.getPath());
        assertTrue(got.isTemplates(), "templates flag must be true after round-trip");
        assertEquals(List.of("foo", "bar"), got.getParameters());
    }

    @Test
    void sourceEntryDefaultTemplatesFalse() throws Exception {
        var src = new SourceEntry();
        src.setName("s2");
        src.setPath("/tmp/s2");
        // templates not set — defaults to false
        var cfg = new ViracochaConfig();
        cfg.getSources().add(src);
        var serialized = yaml.writeValueAsString(cfg);
        var deserialized = yaml.readValue(serialized, ViracochaConfig.class);
        assertFalse(deserialized.getSources().get(0).isTemplates(),
            "templates must default to false when not set in YAML");
    }

    @Test
    void mappingEntryRoundTripWithAllFields() throws Exception {
        var params = new LinkedHashMap<String, String>();
        params.put("k", "v");
        var mapping = new MappingEntry("s1", "**/*.md", true, true, params);
        var dest = new DestinationEntry("d1", "/tmp/d", new LinkedHashMap<>(),
            new ArrayList<>(List.of(mapping)));
        var cfg = new ViracochaConfig();
        cfg.getDestinations().add(dest);
        var serialized = yaml.writeValueAsString(cfg);
        var deserialized = yaml.readValue(serialized, ViracochaConfig.class);
        var gotDest = deserialized.getDestinations().get(0);
        assertEquals("d1", gotDest.getName());
        assertEquals(1, gotDest.getMappings().size());
        var gotMapping = gotDest.getMappings().get(0);
        assertEquals("s1", gotMapping.getSourceRef());
        assertEquals("**/*.md", gotMapping.getGlob());
        assertTrue(gotMapping.isRecurse());
        assertTrue(gotMapping.isSync());
        assertEquals("v", gotMapping.getParams().get("k"));
    }

    @Test
    void mappingEntryDefaultsWhenFieldsAbsent() throws Exception {
        // Write minimal YAML with only sourceRef
        var minimalYaml = "version: 3\nsources: []\ndestinations:\n" +
            "  - name: d1\n    path: /tmp/d\n    mappings:\n" +
            "      - sourceRef: s1\n";
        var deserialized = yaml.readValue(minimalYaml, ViracochaConfig.class);
        var got = deserialized.getDestinations().get(0).getMappings().get(0);
        assertNull(got.getGlob(), "glob must be null when not present in YAML");
        assertFalse(got.isRecurse(), "recurse must default to false");
        assertFalse(got.isSync(), "sync must default to false");
        assertNotNull(got.getParams(), "params must not be null");
    }

    @Test
    void destinationEntryParametersRoundTrip() throws Exception {
        var dParams = new LinkedHashMap<String, String>();
        dParams.put("key", "val");
        var dest = new DestinationEntry("d1", "/tmp/d", dParams, new ArrayList<>());
        var cfg = new ViracochaConfig();
        cfg.getDestinations().add(dest);
        var serialized = yaml.writeValueAsString(cfg);
        var deserialized = yaml.readValue(serialized, ViracochaConfig.class);
        assertEquals("val", deserialized.getDestinations().get(0).getParameters().get("key"));
    }
}
