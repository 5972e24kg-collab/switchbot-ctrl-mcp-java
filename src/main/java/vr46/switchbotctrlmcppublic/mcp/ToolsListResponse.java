package vr46.switchbotctrlmcppublic.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*サンプル
{
    "jsonrpc": "2.0",
    "id": 2,
    "result": {
        "tools": [
            {
                "name": "ping",
                "description": "Health check tool. Returns a simple pong response from this MCP server.",
                "inputSchema": {
                    "type": "object",
                    "properties": {},
                    "required": [],
                    "additionalProperties": false
                }
            },
            {
                "name": "listEnvironments",
                "description": "List registered Docker environments in a compact JSON format. This implementation returns dummy resource JSON for MCP learning.",
                "inputSchema": {
                    "type": "object",
                    "properties": {},
                    "required": [],
                    "additionalProperties": false
                }
            },
            {
                "name": "listContainers",
                "description": "List containers for a specified environmentName in a compact JSON format. This implementation returns dummy resource JSON for MCP learning.",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "environmentName": {
                            "type": "string",
                            "description": "Target environment name. Example: myHost or local."
                        },
                        "all": {
                            "type": "boolean",
                            "description": "Whether to include stopped containers. Dummy implementation accepts the value but does not filter.",
                            "default": true
                        }
                    },
                    "required": [
                        "environmentName"
                    ],
                    "additionalProperties": false
                }
            }
        ]
    }
}
 */
public class ToolsListResponse {
    @JsonProperty("jsonrpc")
    public String jsonrpc = "";
    @JsonProperty("id")
    public int id = 0;
    @JsonProperty("result")
    public Result result = new Result();

    public static class Result {
        @JsonProperty("tools")
        public List<Object> tools = new ArrayList<>();
        public void addTool(Object value) {
            this.tools.add(value);
        }
    }

    public static class Tool {
        @JsonProperty("name")
        public String name = "";
        @JsonProperty("description")
        public String description = "";
        @JsonProperty("inputSchema")
        public InputSchema inputSchema = new InputSchema();
    }

    public static class InputSchema {
        @JsonProperty("type")
        public String type = "";
        @JsonProperty("additionalProperties")
        public boolean additionalProperties = false;    //定義していない余計な引数を許すかどうか
        @JsonProperty("required")
        public List<String> required = new ArrayList<>();   //必須項目のリスト
        public void addRequired(String name) {
            this.required.add(name);
        }
        @JsonProperty("properties")
        public Map<String, Property> properties = new LinkedHashMap<>();
        public void addProperty(String name, Property property) {
            this.properties.put(name, property);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Property {
        @JsonProperty("type")
        public String type = "";
        @JsonProperty("description")
        public String description = "";
        @JsonProperty("default")
        public Object defaultValue = null;

        public Property() {
        }

        public Property(String type, String description) {
            this.type = type;
            this.description = description;
        }

        public Property(String type, String description, Object defaultValue) {
            this.type = type;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        public static Property string(String description) {
            return new Property("string", description);
        }
        public static Property bool(String description) {
            return new Property("boolean", description);
        }
        public static Property bool(String description, boolean defaultValue) {
            return new Property("boolean", description, defaultValue);
        }
        public static Property number(String description) {
            return new Property("number", description);
        }
        public static Property integer(String description) {
            return new Property("integer", description);
        }
    }
}