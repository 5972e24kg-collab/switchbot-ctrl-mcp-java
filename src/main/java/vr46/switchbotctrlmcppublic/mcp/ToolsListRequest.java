package vr46.switchbotctrlmcppublic.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/*
{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "tools/list",
    "params": {}
}
 */
public class ToolsListRequest {
    private final String jsonrpc;
    private final Integer id;
    private final String method;
    private final Params params;

    public ToolsListRequest(String jsonrpc, Integer id, String method, Params params) {
        this.jsonrpc = jsonrpc;
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public ToolsListRequest(JsonNode root) {
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

    public static class Params {
        public Params() {
        }

        public Params(JsonNode node) {
            // 現状フィールドを持たないため、親コンストラクタのみ呼び出します
            this();
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
}