package org.saltations.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A named project with workspace root path and pattern mappings.
 * Serialized to/from YAML by jackson-dataformat-yaml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEntry {

    private String name;
    private String path;
    private List<MappingEntry> mappings = new ArrayList<>();
}
