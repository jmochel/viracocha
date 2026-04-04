package org.saltations.model;

/**
 * Sync direction for a subscription. YAML stores these enum constant names.
 */
public enum SubscriptionSyncDirection {

    PUBLISH_TO_WORKSPACE,
    WORKSPACE_TO_PUBLISH,
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
            case "publish-to-workspace" -> PUBLISH_TO_WORKSPACE;
            case "workspace-to-publish" -> WORKSPACE_TO_PUBLISH;
            case "bidirectional" -> BIDIRECTIONAL;
            default -> throw new IllegalArgumentException("Invalid direction: " + t);
        };
    }
}
