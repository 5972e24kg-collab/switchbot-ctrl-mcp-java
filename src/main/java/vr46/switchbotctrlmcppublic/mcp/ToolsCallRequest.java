package vr46.switchbotctrlmcppublic.mcp;

import com.fasterxml.jackson.databind.JsonNode;
/*
{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
}
 */
public class ToolsCallRequest {
    private final String jsonrpc;
    private final Integer id;
    private final String method;
    private final Params params;

    public ToolsCallRequest(String jsonrpc, Integer id, String method, Params params) {
        this.jsonrpc = jsonrpc;
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public ToolsCallRequest(JsonNode root) {
        this(
                extractText(root, "jsonrpc"),
                extractInteger(root, "id"),
                extractText(root, "method"),
                extractParams(root)
        );
    }

    public String getJsonrpc() {
        return jsonrpc;
    }
    public Integer getId() {
        return id;
    }
    public String getMethod() {
        return method;
    }
    public Params getParams() {
        return params;
    }
    public Arguments getArguments() {
        return this.params != null ? this.params.getArguments() : null;
    }

    public static class Params {
        private final String name;
        private final Arguments arguments;

        public Params(String name, Arguments arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        public Params(JsonNode node) {
            this(
                    extractText(node, "name"),
                    extractArguments(node)
            );
        }

        public String getName() {
            return name;
        }
        public Arguments getArguments() {
            return arguments;
        }
    }

    public static class Arguments {
        private final JsonNode rawNode;
        public Arguments(JsonNode rawNode) {
            this.rawNode = rawNode;
        }
        public JsonNode getRawNode() {
            return rawNode;
        }

        public String getValue(String key) {
            if (rawNode == null) return null;
            JsonNode target = rawNode.path(key);
            if (target.isMissingNode() || target.isNull()) {
                return null;
            }
            return target.asText();
        }

        public Boolean getBoolean(String key) {
            if (rawNode == null) return null;
            JsonNode target = rawNode.path(key);
            return target.isBoolean() ? target.asBoolean() : null;
        }

        public Integer getInteger(String key) {
            if (rawNode == null) return null;
            JsonNode target = rawNode.path(key);
            return target.isNumber() ? target.asInt() : null;
        }
    }

    private static String extractText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode target = node.path(field);
        return target.isTextual() ? target.asText() : null;
    }

    private static Integer extractInteger(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode target = node.path(field);
        return target.isNumber() ? target.asInt() : null;
    }

    private static Params extractParams(JsonNode node) {
        if (node == null) return null;
        JsonNode target = node.path("params");
        return target.isObject() ? new Params(target) : null;
    }

    private static Arguments extractArguments(JsonNode node) {
        if (node == null) return null;
        JsonNode target = node.path("arguments");
        return target.isMissingNode() ? new Arguments(null) : new Arguments(target);
    }
}
