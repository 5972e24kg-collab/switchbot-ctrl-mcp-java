package vr46.switchbotctrlmcppublic.mcp;

import com.fasterxml.jackson.databind.JsonNode;
/* サンプル
{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
        "protocolVersion": "2025-06-18",
        "capabilities": {},
        "clientInfo": {
            "name": "curl-test-client",
            "version": "0.1.0"
        }
    }
}
 */
public class InitializeRequest {
    private final String jsonrpc;
    private final Integer id;
    private final String method;
    private final Params params;

    public InitializeRequest(String jsonrpc, Integer id, String method, Params params) {
        this.jsonrpc = jsonrpc;
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public InitializeRequest(JsonNode root) {
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
        private final String protocolVersion;
        private final JsonNode capabilities;
        private final ClientInfo clientInfo;

        public Params(String protocolVersion, JsonNode capabilities, ClientInfo clientInfo) {
            this.protocolVersion = protocolVersion;
            this.capabilities = capabilities;
            this.clientInfo = clientInfo;
        }

        public Params(JsonNode node) {
            this(
                    extractText(node, "protocolVersion"),
                    extractNode(node, "capabilities"),
                    extractClientInfo(node)
            );
        }

        public String getProtocolVersion() {
            return protocolVersion;
        }
        public JsonNode getCapabilities() {
            return capabilities;
        }
        public ClientInfo getClientInfo() {
            return clientInfo;
        }
    }

    public static class ClientInfo {
        private final String name;
        private final String version;

        public ClientInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public ClientInfo(JsonNode node) {
            this(
                    extractText(node, "name"),
                    extractText(node, "version")
            );
        }

        public String getName() {
            return name;
        }
        public String getVersion() {
            return version;
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

    private static JsonNode extractNode(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode target = node.path(field);
        return (target.isMissingNode() || target.isNull()) ? null : target;
    }

    private static Params extractParams(JsonNode node) {
        JsonNode paramsNode = extractNode(node, "params");
        return paramsNode != null ? new Params(paramsNode) : null;
    }

    private static ClientInfo extractClientInfo(JsonNode node) {
        JsonNode clientInfoNode = extractNode(node, "clientInfo");
        return clientInfoNode != null ? new ClientInfo(clientInfoNode) : null;
    }
}