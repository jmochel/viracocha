package org.saltations.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Central config entry for a registered pattern.
 * parameters holds extracted Freemarker variable names (sorted, deduplicated).
 * Serialized to/from YAML by jackson-dataformat-yaml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatternEntry {
    private String name;
    private String path;
    private List<String> parameters = new ArrayList<>();
}
