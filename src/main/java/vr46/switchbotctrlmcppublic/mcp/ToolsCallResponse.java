package vr46.switchbotctrlmcppublic.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/*
{
    "jsonrpc": "2.0",
    "id": 3,
    "result": {
        "content": [
            {
                "type": "text",
                "text": "{\"message\":\"pong\",\"server\":\"portainer-ops-mcp-java\",\"status\":\"ok\"}"
            }
        ],
        "isError": false
    }
}
 */
public class ToolsCallResponse {
    @JsonProperty("jsonrpc")
    public String jsonrpc = "";
    @JsonProperty("id")
    public int id = 0;
    @JsonProperty("result")
    public Result result = new Result();

    public static class Result {
        @JsonProperty("content")
        public List<Object> contents = new ArrayList<>();
        public void addContent(Object value) {
            this.contents.add(value);
        }
        @JsonProperty("isError")
        public boolean isError = false;
    }

    public static class Content {
        @JsonProperty("type")
        public String type = "text";
        @JsonProperty("text")
        public String text = "";
    }
}
