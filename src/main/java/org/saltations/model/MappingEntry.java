package org.saltations.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One source-to-destination mapping. Belongs to a DestinationEntry.
 * Serialized to/from YAML by jackson-dataformat-yaml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingEntry {
    /** Name of the SourceEntry this mapping reads from. */
    private String sourceRef;
    /** Glob pattern filter (null = no filter = copy all files). */
    private String glob = null;
    /** When true, walk source directory recursively. Default false (flat copy). */
    private boolean recurse = false;
    /** When true, keep destination files in sync with source on 'vira sync'. Default false. */
    private boolean sync = false;
    /** Per-mapping parameter overrides for template expansion. */
    private Map<String, String> params = new LinkedHashMap<>();
}
