package org.saltations.model;

/**
 * Sync direction for a subscription. YAML stores these enum constant names.
 */
public enum SubscriptionSyncDirection {

    CATALOG_TO_WORKSPACE,
    WORKSPACE_TO_CATALOG,
    BIDIRECTIONAL;

    /**
     * Parses a CLI kebab-case direction string (case-insensitive).
     *
     * @param s raw token from {@code --direction}
     * @return matching enum constant
     * @throws IllegalArgumentException if null, blank after trim, or unrecognized
     */
    public static SubscriptionSyncDirection fromCliKebab(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Invalid direction: null");
        }
        String t = s.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("Invalid direction: (empty)");
        }
        String lower = t.toLowerCase();
        return switch (lower) {
            case "catalog-to-workspace" -> CATALOG_TO_WORKSPACE;
            case "workspace-to-catalog" -> WORKSPACE_TO_CATALOG;
            case "bidirectional" -> BIDIRECTIONAL;
            default -> throw new IllegalArgumentException("Invalid direction: " + t);
        };
    }
}
