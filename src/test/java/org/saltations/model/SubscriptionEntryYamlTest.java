package org.saltations.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * YAML round-trip for {@link SubscriptionEntry} nested under {@link ProjectEntry}.
 */
class SubscriptionEntryYamlTest {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    @Test
    void subscriptionRoundTripsUnderProject() throws Exception {
        ViracochaConfig config = new ViracochaConfig();
        SubscriptionEntry sub = new SubscriptionEntry(
            "550e8400-e29b-41d4-a716-446655440000",
            "pub1",
            "src",
            "out",
            SubscriptionSyncDirection.PUBLISH_TO_WORKSPACE);
        config.getProjects().add(new ProjectEntry("p1", "/ws", List.of(), new LinkedHashMap<>(), List.of(sub)));

        String serialized = yaml.writeValueAsString(config);
        ViracochaConfig deserialized = yaml.readValue(serialized, ViracochaConfig.class);

        assertEquals(1, deserialized.getProjects().size());
        ProjectEntry p = deserialized.getProjects().get(0);
        assertEquals(1, p.getSubscriptions().size());
        SubscriptionEntry s = p.getSubscriptions().get(0);
        assertEquals(SubscriptionSyncDirection.PUBLISH_TO_WORKSPACE, s.getDirection());
        assertEquals("pub1", s.getPublisherName());
    }
}
