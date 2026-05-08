package org.saltations.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A named destination workspace path with parameter defaults and source mappings.
 * Serialized to/from YAML by jackson-dataformat-yaml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DestinationEntry {
    private String name;
    private String path;
    /** Default parameter values for template expansion at this destination. */
    private Map<String, String> parameters = new LinkedHashMap<>();
    /** Mappings from sources into this destination. */
    private List<MappingEntry> mappings = new ArrayList<>();
}
