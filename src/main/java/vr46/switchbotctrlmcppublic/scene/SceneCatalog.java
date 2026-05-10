package vr46.switchbotctrlmcppublic.scene;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SceneCatalog {
    public static final String DEFAULT_RESOURCE_NAME = "sceneCatalog.json";

    private static final TypeReference<List<SceneCatalogEntry>> ENTRY_LIST_TYPE = new TypeReference<>() {
    };

    private final String resourceName;
    private final ObjectMapper objectMapper;
    private final ClassLoader classLoader;

    public SceneCatalog() {
        this(DEFAULT_RESOURCE_NAME);
    }

    public SceneCatalog(String resourceName) {
        this(resourceName, new ObjectMapper(), defaultClassLoader());
    }

    SceneCatalog(String resourceName, ObjectMapper objectMapper, ClassLoader classLoader) {
        this.resourceName = requireNonBlank(resourceName, "resourceName");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null.");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null.");
    }

    public List<SceneCatalogEntry> loadEntries() {
        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new SceneCatalogException("Scene catalog resource was not found: " + resourceName);
            }
            try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                JsonNode root = objectMapper.readTree(reader);
                validate(root);
                return List.copyOf(objectMapper.convertValue(root, ENTRY_LIST_TYPE));
            }
        } catch (SceneCatalogException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new SceneCatalogException("Failed to parse scene catalog JSON: " + resourceName, e);
        } catch (IllegalArgumentException e) {
            throw new SceneCatalogException("Failed to map scene catalog JSON: " + resourceName, e);
        } catch (IOException e) {
            throw new SceneCatalogException("Failed to read scene catalog resource: " + resourceName, e);
        }
    }

    private static ClassLoader defaultClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader == null ? SceneCatalog.class.getClassLoader() : contextClassLoader;
    }

    private static void validate(JsonNode root) {
        if (root == null || !root.isArray()) {
            throw new SceneCatalogException("Scene catalog JSON must be an array.");
        }

        Set<String> sceneKeys = new HashSet<>();
        Set<String> sceneIds = new HashSet<>();
        for (int i = 0; i < root.size(); i++) {
            JsonNode entry = root.get(i);
            if (entry == null || !entry.isObject()) {
                throw new SceneCatalogException("Scene catalog entry must be an object at index " + i + ".");
            }

            String sceneKey = requireText(entry, i, "sceneKey");
            String sceneId = requireText(entry, i, "sceneId");
            requireText(entry, i, "sceneName");
            requireText(entry, i, "publicName");
            requireText(entry, i, "category");
            requireText(entry, i, "action");
            requireText(entry, i, "riskLevel");
            requireBoolean(entry, i, "executable");
            requireAliases(entry, i);
            validateCooldown(entry, i);

            if (!sceneKeys.add(sceneKey)) {
                throw new SceneCatalogException("Duplicate sceneKey in scene catalog: " + sceneKey);
            }
            if (!sceneIds.add(sceneId)) {
                throw new SceneCatalogException("Duplicate sceneId in scene catalog: " + sceneId);
            }
        }
    }

    private static void validateCooldown(JsonNode entry, int index) {
        JsonNode value = entry.get("cooldown");
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new SceneCatalogException("cooldown must be an integer in scene catalog at index " + index + ".");
        }
        if (value.asInt() < 0) {
            throw new SceneCatalogException("cooldown must not be negative in scene catalog at index " + index + ".");
        }
    }

    private static String requireText(JsonNode entry, int index, String fieldName) {
        JsonNode value = entry.get(fieldName);
        if (value == null || value.isNull()) {
            throw new SceneCatalogException("Required field is missing in scene catalog at index " + index + ": " + fieldName);
        }
        if (!value.isTextual()) {
            throw new SceneCatalogException("Required field must be a string in scene catalog at index " + index + ": " + fieldName);
        }
        String text = value.asText();
        if (text.isBlank()) {
            throw new SceneCatalogException("Required field must not be blank in scene catalog at index " + index + ": " + fieldName);
        }
        return text;
    }

    private static void requireBoolean(JsonNode entry, int index, String fieldName) {
        JsonNode value = entry.get(fieldName);
        if (value == null || value.isNull()) {
            throw new SceneCatalogException("Required field is missing in scene catalog at index " + index + ": " + fieldName);
        }
        if (!value.isBoolean()) {
            throw new SceneCatalogException("Required field must be a boolean in scene catalog at index " + index + ": " + fieldName);
        }
    }

    private static void requireAliases(JsonNode entry, int index) {
        JsonNode aliases = entry.get("aliases");
        if (aliases == null || aliases.isNull()) {
            throw new SceneCatalogException("Required field is missing in scene catalog at index " + index + ": aliases");
        }
        if (!aliases.isArray()) {
            throw new SceneCatalogException("aliases must be an array in scene catalog at index " + index + ".");
        }
        for (int i = 0; i < aliases.size(); i++) {
            JsonNode alias = aliases.get(i);
            if (alias == null || !alias.isTextual()) {
                throw new SceneCatalogException("aliases must contain only strings in scene catalog at index " + index + ", aliases[" + i + "].");
            }
        }
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null.");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }
}
