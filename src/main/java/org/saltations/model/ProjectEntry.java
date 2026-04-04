package org.saltations.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    /** Default parameter values for this project; mapping-level params override at generation time. */
    private Map<String, String> parameters = new LinkedHashMap<>();
    /** Optional subscription rows for publisher↔workspace sync (Phase 6+). */
    private List<SubscriptionEntry> subscriptions = new ArrayList<>();
}
