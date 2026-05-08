package org.saltations.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Root configuration POJO for viracocha central config.yaml (v3 schema).
 * Serialized/deserialized with jackson-dataformat-yaml ObjectMapper.
 * Do NOT use as a Micronaut @ConfigurationProperties — this is user domain data.
 */
@Data
public class ViracochaConfig {
    /** Schema version. Must be 3 for v3 format. */
    private int version = 3;
    private List<SourceEntry> sources = new ArrayList<>();
    private List<DestinationEntry> destinations = new ArrayList<>();
}
