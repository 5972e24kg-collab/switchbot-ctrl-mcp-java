package vr46.switchbotctrlmcppublic.scene;

import vr46.switchbotctrlmcppublic.converter.SwitchBotScenesConvertException;
import vr46.switchbotctrlmcppublic.converter.SwitchBotScenesConverter;
import vr46.switchbotctrlmcppublic.dto.SceneSummary;
import vr46.switchbotctrlmcppublic.switchbot.SwitchBotApiException;
import vr46.switchbotctrlmcppublic.switchbot.SwitchBotApiResult;
import vr46.switchbotctrlmcppublic.switchbot.SwitchBotApisV2;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SceneService {
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final SwitchBotApisV2 switchBotApisV2;
    private final SceneCatalog sceneCatalog;
    private final SwitchBotScenesConverter converter;
    private final boolean dryRun;
    private final Clock clock;
    private final Object lock = new Object();
    private final Map<String, Instant> lastExecutedAtBySceneKey = new HashMap<>();

    private List<SceneSummary> cachedScenes;
    private Instant cachedAt;

    public SceneService(SwitchBotApisV2 switchBotApisV2) {
        this(switchBotApisV2, false);
    }

    public SceneService(SwitchBotApisV2 switchBotApisV2, boolean dryRun) {
        this(switchBotApisV2, new SceneCatalog(), new SwitchBotScenesConverter(), dryRun);
    }

    public SceneService(
            SwitchBotApisV2 switchBotApisV2,
            SceneCatalog sceneCatalog,
            SwitchBotScenesConverter converter,
            boolean dryRun
    ) {
        this(switchBotApisV2, sceneCatalog, converter, dryRun, Clock.systemUTC());
    }

    SceneService(
            SwitchBotApisV2 switchBotApisV2,
            SceneCatalog sceneCatalog,
            SwitchBotScenesConverter converter,
            boolean dryRun,
            Clock clock
    ) {
        this.switchBotApisV2 = Objects.requireNonNull(switchBotApisV2, "switchBotApisV2 must not be null.");
        this.sceneCatalog = Objects.requireNonNull(sceneCatalog, "sceneCatalog must not be null.");
        this.converter = Objects.requireNonNull(converter, "converter must not be null.");
        this.dryRun = dryRun;
        this.clock = Objects.requireNonNull(clock, "clock must not be null.");
    }

    public List<SceneSummary> listScenes() {
        Instant now = Instant.now(clock);
        synchronized (lock) {
            if (isCacheValid(now)) {
                return cachedScenes;
            }
        }
        return refreshScenes();
    }

    public List<SceneSummary> refreshScenes() {
        List<SceneSummary> freshScenes = loadScenes();
        Instant refreshedAt = Instant.now(clock);
        synchronized (lock) {
            cachedScenes = freshScenes;
            cachedAt = refreshedAt;
        }
        return freshScenes;
    }

    public SceneExecutionResult executeBySceneKey(String sceneKey) {
        String normalizedSceneKey = normalizeSceneKey(sceneKey);
        SceneSummary sceneSummary = findScene(normalizedSceneKey);
        if (!sceneSummary.executable()) {
            throw new SceneServiceException("Scene is not executable: " + normalizedSceneKey);
        }

        Instant now = Instant.now(clock);
        if (isCooldownActive(sceneSummary, now)) {
            return new SceneExecutionResult(
                    sceneSummary.sceneKey(),
                    sceneSummary.sceneId(),
                    sceneSummary.sceneName(),
                    false,
                    0,
                    "",
                    "Cooldown active.",
                    false,
                    false
            );
        }

        if (dryRun) {
            return new SceneExecutionResult(
                    sceneSummary.sceneKey(),
                    sceneSummary.sceneId(),
                    sceneSummary.sceneName(),
                    true,
                    0,
                    "",
                    "Dry-run: scene execution was skipped.",
                    false,
                    true
            );
        }

        SwitchBotApiResult result;
        try {
            result = switchBotApisV2.executeScene(sceneSummary.sceneId());
        } catch (SwitchBotApiException e) {
            throw new SceneServiceException("Failed to execute SwitchBot scene: " + normalizedSceneKey, e);
        }

        synchronized (lock) {
            lastExecutedAtBySceneKey.put(normalizedSceneKey, Instant.now(clock));
        }

        return new SceneExecutionResult(
                sceneSummary.sceneKey(),
                sceneSummary.sceneId(),
                sceneSummary.sceneName(),
                result.success(),
                result.statusCode(),
                result.body(),
                result.success() ? "Scene executed." : "Scene execution failed.",
                true,
                false
        );
    }

    private List<SceneSummary> loadScenes() {
        try {
            SwitchBotApiResult result = switchBotApisV2.getScenes();
            if (!result.success()) {
                throw new SceneServiceException("Failed to get SwitchBot scenes. httpStatusCode=" + result.statusCode());
            }

            List<SceneCatalogEntry> catalogEntries = sceneCatalog.loadEntries();
            return List.copyOf(converter.convert(result.body(), catalogEntries));
        } catch (SceneServiceException e) {
            throw e;
        } catch (SceneCatalogException e) {
            throw new SceneServiceException("Failed to load scene catalog.", e);
        } catch (SwitchBotScenesConvertException e) {
            throw new SceneServiceException("Failed to convert SwitchBot scenes.", e);
        } catch (SwitchBotApiException e) {
            throw new SceneServiceException("Failed to get SwitchBot scenes.", e);
        }
    }

    private boolean isCacheValid(Instant now) {
        return cachedScenes != null
                && cachedAt != null
                && now.isBefore(cachedAt.plus(CACHE_TTL));
    }

    private SceneSummary findScene(String sceneKey) {
        for (SceneSummary sceneSummary : listScenes()) {
            if (sceneKey.equals(sceneSummary.sceneKey())) {
                return sceneSummary;
            }
        }
        throw new SceneServiceException("Scene key is not registered as a public executable scene: " + sceneKey);
    }

    private boolean isCooldownActive(SceneSummary sceneSummary, Instant now) {
        if (sceneSummary.cooldown() <= 0) {
            return false;
        }
        synchronized (lock) {
            Instant lastExecutedAt = lastExecutedAtBySceneKey.get(sceneSummary.sceneKey());
            return lastExecutedAt != null && now.isBefore(lastExecutedAt.plusSeconds(sceneSummary.cooldown()));
        }
    }

    private static String normalizeSceneKey(String sceneKey) {
        if (sceneKey == null) {
            throw new SceneServiceException("sceneKey must not be null.");
        }
        String normalizedSceneKey = sceneKey.trim();
        if (normalizedSceneKey.isBlank()) {
            throw new SceneServiceException("sceneKey must not be blank.");
        }
        return normalizedSceneKey;
    }
}
