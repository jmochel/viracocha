package org.saltations.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One pattern-to-workspace-path mapping under a project.
 * Serialized to/from YAML by jackson-dataformat-yaml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingEntry {

    private String patternName;
    /** Relative path under the project workspace root where the pattern is installed. */
    private String workspacePath;
    private Map<String, String> parameters = new LinkedHashMap<>();
}
