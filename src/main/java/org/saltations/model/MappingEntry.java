package org.saltations.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One pattern-to-destination mapping under a project.
 * Serialized to/from YAML by jackson-dataformat-yaml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingEntry {

    private String patternName;
    private String destination;
    private Map<String, String> parameters = new LinkedHashMap<>();
}
