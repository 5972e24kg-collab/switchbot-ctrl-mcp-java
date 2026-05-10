package vr46.switchbotctrlmcppublic.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import vr46.switchbotctrlmcppublic.dto.SceneSummary;
import vr46.switchbotctrlmcppublic.scene.SceneCatalogEntry;

import java.util.*;

public final class SwitchBotScenesConverter {
    private static final int SUCCESS_STATUS_CODE = 100;

    private final ObjectMapper objectMapper;

    public SwitchBotScenesConverter() {
        this(new ObjectMapper());
    }

    SwitchBotScenesConverter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null.");
    }

    public List<SceneSummary> convert(String rawJson, List<SceneCatalogEntry> catalogEntries) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes raw JSON must not be blank.");
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            validateRawResponse(root);
            SwitchBotScenesResponse response = objectMapper.treeToValue(root, SwitchBotScenesResponse.class);
            return convert(response, catalogEntries);
        } catch (SwitchBotScenesConvertException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new SwitchBotScenesConvertException("Failed to parse SwitchBot getScenes raw JSON.", e);
        } catch (IllegalArgumentException e) {
            throw new SwitchBotScenesConvertException("Failed to map SwitchBot getScenes raw JSON.", e);
        }
    }

    public List<SceneSummary> convert(SwitchBotScenesResponse response, List<SceneCatalogEntry> catalogEntries) {
        Objects.requireNonNull(response, "response must not be null.");
        Objects.requireNonNull(catalogEntries, "catalogEntries must not be null.");
        validateResponse(response);

        Map<String, SwitchBotScenesResponse.Scene> apiScenesBySceneId = new HashMap<>();
        for (SwitchBotScenesResponse.Scene apiScene : response.body()) {
            validateApiScene(apiScene);
            SwitchBotScenesResponse.Scene previous = apiScenesBySceneId.put(apiScene.sceneId(), apiScene);
            if (previous != null) {
                throw new SwitchBotScenesConvertException("Duplicate sceneId in SwitchBot getScenes response: " + apiScene.sceneId());
            }
        }

        List<SceneSummary> summaries = new ArrayList<>();
        for (SceneCatalogEntry catalogEntry : catalogEntries) {
            validateCatalogEntry(catalogEntry);
            if (!catalogEntry.executable()) {
                continue;
            }

            SwitchBotScenesResponse.Scene apiScene = apiScenesBySceneId.get(catalogEntry.sceneId());
            if (apiScene == null) {
                continue;
            }

            // sceneName mismatches are accepted; the SwitchBot API name is the current real name.
            summaries.add(new SceneSummary(
                    catalogEntry.sceneKey(),
                    catalogEntry.sceneId(),
                    apiScene.sceneName(),
                    catalogEntry.publicName(),
                    catalogEntry.category(),
                    catalogEntry.action(),
                    catalogEntry.riskLevel(),
                    catalogEntry.executable(),
                    catalogEntry.aliases(),
                    normalizeCooldown(catalogEntry.cooldown())
            ));
        }
        return List.copyOf(summaries);
    }

    private static void validateRawResponse(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes raw JSON must be an object.");
        }
        JsonNode statusCode = root.get("statusCode");
        if (statusCode == null || statusCode.isNull()) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes raw JSON is missing statusCode.");
        }
        if (!statusCode.isInt()) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes statusCode must be an integer.");
        }

        JsonNode body = root.get("body");
        if (body == null || body.isNull()) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes raw JSON is missing body.");
        }
        if (!body.isArray()) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes body must be an array.");
        }
        for (int i = 0; i < body.size(); i++) {
            JsonNode scene = body.get(i);
            if (scene == null || !scene.isObject()) {
                throw new SwitchBotScenesConvertException("SwitchBot getScenes body entry must be an object at index " + i + ".");
            }
            requireText(scene, i, "sceneId");
            requireText(scene, i, "sceneName");
        }
    }

    private static void validateResponse(SwitchBotScenesResponse response) {
        if (response.statusCode() == null) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes statusCode is missing.");
        }
        if (response.statusCode() != SUCCESS_STATUS_CODE) {
            throw new SwitchBotScenesConvertException(
                    "SwitchBot getScenes failed. statusCode=" + response.statusCode() + ", message=" + response.message()
            );
        }
        if (response.body() == null) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes body is missing.");
        }
    }

    private static void validateApiScene(SwitchBotScenesResponse.Scene apiScene) {
        if (apiScene == null) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes body contains null scene.");
        }
        requireNonBlank(apiScene.sceneId(), "SwitchBot getScenes sceneId");
        requireNonBlank(apiScene.sceneName(), "SwitchBot getScenes sceneName");
    }

    private static void validateCatalogEntry(SceneCatalogEntry catalogEntry) {
        if (catalogEntry == null) {
            throw new SwitchBotScenesConvertException("catalogEntries contains null entry.");
        }
        requireNonBlank(catalogEntry.sceneKey(), "catalog sceneKey");
        requireNonBlank(catalogEntry.sceneId(), "catalog sceneId");
        requireNonBlank(catalogEntry.sceneName(), "catalog sceneName");
        requireNonBlank(catalogEntry.publicName(), "catalog publicName");
        requireNonBlank(catalogEntry.category(), "catalog category");
        requireNonBlank(catalogEntry.action(), "catalog action");
        requireNonBlank(catalogEntry.riskLevel(), "catalog riskLevel");
        normalizeCooldown(catalogEntry.cooldown());
    }

    private static int normalizeCooldown(Integer cooldown) {
        if (cooldown == null) {
            return SceneSummary.DEFAULT_COOLDOWN;
        }
        if (cooldown < 0) {
            throw new SwitchBotScenesConvertException("catalog cooldown must not be negative.");
        }
        return cooldown;
    }

    private static String requireText(JsonNode scene, int index, String fieldName) {
        JsonNode value = scene.get(fieldName);
        if (value == null || value.isNull()) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes body entry is missing " + fieldName + " at index " + index + ".");
        }
        if (!value.isTextual()) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes body entry field must be a string at index " + index + ": " + fieldName);
        }
        String text = value.asText();
        if (text.isBlank()) {
            throw new SwitchBotScenesConvertException("SwitchBot getScenes body entry field must not be blank at index " + index + ": " + fieldName);
        }
        return text;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new SwitchBotScenesConvertException(name + " must not be blank.");
        }
        return value;
    }
}
