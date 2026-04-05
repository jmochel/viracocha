package org.saltations.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Root configuration POJO for viracocha central config.yaml.
 * Serialized/deserialized with jackson-dataformat-yaml ObjectMapper.
 * Do NOT use as a Micronaut @ConfigurationProperties — this is user domain data.
 */
@Data
public class ViracochaConfig {

    private int version = 1;
    private List<CatalogEntry> catalogs = new ArrayList<>();
    private List<PatternEntry> patterns = new ArrayList<>();
    private List<ProjectEntry> projects = new ArrayList<>();
}
