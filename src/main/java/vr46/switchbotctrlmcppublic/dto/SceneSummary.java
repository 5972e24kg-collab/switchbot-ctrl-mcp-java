package vr46.switchbotctrlmcppublic.dto;

import java.util.List;

public record SceneSummary(
        String sceneKey,
        String sceneId,
        String sceneName,
        String publicName,
        String category,
        String action,
        String riskLevel,
        boolean executable,
        List<String> aliases,
        int cooldown
) {
    public static final int DEFAULT_COOLDOWN = 10;

    public SceneSummary {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        if (cooldown < 0) {
            throw new IllegalArgumentException("cooldown must not be negative.");
        }
    }

    public SceneSummary(
            String sceneKey,
            String sceneId,
            String sceneName,
            String publicName,
            String category,
            String action,
            String riskLevel,
            boolean executable,
            List<String> aliases
    ) {
        this(sceneKey, sceneId, sceneName, publicName, category, action, riskLevel, executable, aliases, DEFAULT_COOLDOWN);
    }
}
