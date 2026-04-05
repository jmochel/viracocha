package org.saltations.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One catalog↔workspace subscription row under a project.
 * Serialized to/from YAML by jackson-dataformat-yaml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEntry {

    private String id;
    private String catalogName;
    private String sourcePath;
    /** Path relative to the project workspace root (subscription "workspace" subtree). */
    private String workspacePath;
    private SubscriptionSyncDirection direction;
}
