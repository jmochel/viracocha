package org.saltations.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A named local directory source. May contain Freemarker templates.
 * Serialized to/from YAML by jackson-dataformat-yaml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceEntry {
    private String name;
    private String path;
    /** When true, files in this source contain Freemarker templates. Default false. */
    private boolean templates = false;
    /** Freemarker variable names extracted from template files. Variable names only, not values. */
    private List<String> parameters = new ArrayList<>();
}
