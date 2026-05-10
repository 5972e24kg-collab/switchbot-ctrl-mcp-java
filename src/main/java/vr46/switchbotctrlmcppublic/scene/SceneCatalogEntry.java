package vr46.switchbotctrlmcppublic.scene;

import java.util.List;

public record SceneCatalogEntry(
        String sceneKey,
        String sceneId,
        String sceneName,
        String publicName,
        String category,
        String action,
        String riskLevel,
        boolean executable,
        List<String> aliases,
        Integer cooldown,
        String note
) {
    public SceneCatalogEntry {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    public SceneCatalogEntry(
            String sceneKey,
            String sceneId,
            String sceneName,
            String publicName,
            String category,
            String action,
            String riskLevel,
            boolean executable,
            List<String> aliases,
            String note
    ) {
        this(sceneKey, sceneId, sceneName, publicName, category, action, riskLevel, executable, aliases, null, note);
    }
}
