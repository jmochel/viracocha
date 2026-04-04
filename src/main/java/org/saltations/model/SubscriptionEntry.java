package org.saltations.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One publisher↔workspace subscription row under a project.
 * Serialized to/from YAML by jackson-dataformat-yaml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEntry {

    private String id;
    private String publisherName;
    private String sourcePath;
    private String destinationPath;
    private SubscriptionSyncDirection direction;
}
