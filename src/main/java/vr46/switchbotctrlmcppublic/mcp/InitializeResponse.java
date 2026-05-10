package vr46.switchbotctrlmcppublic.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;

/*サンプル
{
    "jsonrpc": "2.0",
    "id": 1,
    "result": {
        "protocolVersion": "2025-06-18",
        "capabilities": {
            "tools": {
                "listChanged": false
            }
        },
        "serverInfo": {
            "name": "portainer-ops-mcp-java",
            "title": "Portainer Ops MCP Java Server",
            "version": "0.1.0"
        },
        "instructions": "Read-only MCP server for Portainer/Docker environment observation."
    }
}
 */
public class InitializeResponse {
    @JsonProperty("jsonrpc")
    public String jsonrpc = "";
    @JsonProperty("id")
    public int id = 0;
    @JsonProperty("result")
    public Result result = new Result();

    public static class Result {
        @JsonProperty("protocolVersion")
        public String protocolVersion = "";
        @JsonProperty("instructions")
        public String instructions = "";
        @JsonProperty("capabilities")
        public Capabilities capabilities = new Capabilities();
        @JsonProperty("serverInfo")
        public ServerInfo serverInfo = new ServerInfo();
    }

    public static class Capabilities {
        @JsonProperty("tools")
        public Tools tools = new Tools();
    }

    public static class Tools {
        @JsonProperty("listChanged")
        public boolean listChanged = false;
    }

    public static class ServerInfo {
        @JsonProperty("name")
        public String name = "";
        @JsonProperty("title")
        public String title = "";
        @JsonProperty("version")
        public String version = "";
    }
}