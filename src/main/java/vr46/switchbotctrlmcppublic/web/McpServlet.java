package vr46.switchbotctrlmcppublic.web;


import com.fasterxml.jackson.databind.ObjectMapper;
import vr46.switchbotctrlmcppublic.config.AppConfig;
import vr46.switchbotctrlmcppublic.dto.SceneSummary;
import vr46.switchbotctrlmcppublic.mcp.*;
import vr46.switchbotctrlmcppublic.scene.SceneExecutionResult;
import vr46.switchbotctrlmcppublic.scene.SceneService;
import vr46.switchbotctrlmcppublic.switchbot.SwitchBotApisV2;

import javax.servlet.annotation.WebServlet;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SwitchBot scene control MCP Servlet.
 *
 * <p>
 * This servlet exposes only public SwitchBot scenes through MCP tools.
 * Device direct control, scene catalog loading, sceneId conversion, cooldown, and dry-run control are handled by
 * lower layers.
 * </p>
 */
@WebServlet(name = "McpServlet", urlPatterns = {"/mcp"})
public class McpServlet extends BaseMcpServlet {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String SERVER_NAME = "switchbot-ctrl-mcp-java";
    private static final String SERVER_TITLE = "SwitchBot Control MCP Java Server";
    private static final String SERVER_VERSION = "0.1.0";

    private static final String TOOL_SCENES = "scenes";
    private static final String TOOL_EXECUTE_SCENE = "executeScene";

    private SceneService sceneService;

    public McpServlet() {
        super(SERVER_VERSION);
    }

    @Override
    public void init() {
        AppConfig config = AppConfig.load();
        SwitchBotApisV2 api = new SwitchBotApisV2(config.TOKEN, config.SECRET);
        this.sceneService = new SceneService(api, false);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // initialize
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    protected InitializeResponse resInitialize(InitializeRequest initializeRequest) {
        InitializeResponse initializeResponse = super.resInitialize(initializeRequest);

        initializeResponse.result.instructions =
                "MCP server for safely listing and executing SwitchBot scenes. "
                        + "This server does not directly control devices; it only handles scenes explicitly approved for MCP exposure. "
                        + "Use 'scenes' to list available public scenes and confirm the sceneKey values that can be executed. "
                        + "Use 'executeScene' with an exact sceneKey returned by 'scenes' to execute a scene. "
                        + "Do not pass or invent sceneId values directly. "
                        + "Execution controls such as cooldown and dry-run are enforced by the server side. "
                        + "'refreshScenes' is intentionally not exposed as an MCP tool.";

        initializeResponse.result.capabilities.tools.listChanged = false;
        initializeResponse.result.serverInfo.name = SERVER_NAME;
        initializeResponse.result.serverInfo.title = SERVER_TITLE;
        initializeResponse.result.serverInfo.version = SERVER_VERSION;

        return initializeResponse;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // tools/list
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    protected ToolsListResponse resToolsList(ToolsListRequest toolsListRequest) {
        ToolsListResponse toolsListResponse = super.resToolsList(toolsListRequest);

        addScenesTool(toolsListResponse);
        addExecuteSceneTool(toolsListResponse);

        return toolsListResponse;
    }

    /**
     * scenes tool.
     */
    private void addScenesTool(ToolsListResponse toolsListResponse) {
        ToolsListResponse.Tool scenes = new ToolsListResponse.Tool();
        scenes.name = TOOL_SCENES;
        scenes.description =
                "List SwitchBot scenes that are available through this MCP server as JSON. "
                        + "Use this tool when the user asks what SwitchBot actions are available, or before executing a scene to confirm available sceneKey values. "
                        + "The response includes only scenes approved for MCP exposure, not every scene in the SwitchBot app. "
                        + "Use the returned sceneKey exactly as the sceneKey argument for the executeScene tool. "
                        + "This tool has no arguments.";

        scenes.inputSchema.type = "object";
        scenes.inputSchema.additionalProperties = false;

        toolsListResponse.result.addTool(scenes);
    }

    /**
     * executeScene tool.
     */
    private void addExecuteSceneTool(ToolsListResponse toolsListResponse) {
        ToolsListResponse.Tool executeScene = new ToolsListResponse.Tool();
        executeScene.name = TOOL_EXECUTE_SCENE;
        executeScene.description =
                "Execute one SwitchBot scene by sceneKey and return execution result JSON. "
                        + "Use a sceneKey returned by the scenes tool. "
                        + "Do not use sceneId, natural language, or aliases as the argument; pass the exact sceneKey. "
                        + "The result includes success, executed, dryRun, and message. "
                        + "The server may skip calling the SwitchBot API when cooldown is active or dry-run is enabled.";

        executeScene.inputSchema.type = "object";
        executeScene.inputSchema.additionalProperties = false;

        executeScene.inputSchema.addProperty(
                "sceneKey",
                ToolsListResponse.Property.string(
                        "Scene key returned by the scenes tool. Example: room_light_off."
                )
        );

        executeScene.inputSchema.addRequired("sceneKey");

        toolsListResponse.result.addTool(executeScene);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // tools/call
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    protected ToolsCallResponse resToolsCall(ToolsCallRequest toolsCallRequest) {
        String toolName = getToolName(toolsCallRequest);

        if (toolName == null) {
            return errorResponse(
                    toolsCallRequest,
                    "Invalid tools/call request.",
                    "Tool name is missing."
            );
        }

        switch (toolName) {
            case TOOL_SCENES:
                return scenesToolResponse(toolsCallRequest);

            case TOOL_EXECUTE_SCENE:
                return executeSceneToolResponse(toolsCallRequest);

            default:
                /*
                 * Unknown tools are handled by BaseMcpServlet.
                 */
                return null;
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // tool implementations
    // -----------------------------------------------------------------------------------------------------------------

    private ToolsCallResponse scenesToolResponse(ToolsCallRequest toolsCallRequest) {
        try {
            List<SceneSummary> scenes = this.sceneService.listScenes();
            return successResponse(toolsCallRequest, scenes);

        } catch (Exception e) {
            logException("scenesToolResponse", e);
            return errorResponse(
                    toolsCallRequest,
                    "Failed to list SwitchBot scenes.",
                    safeMessage(e)
            );
        }
    }

    private ToolsCallResponse executeSceneToolResponse(ToolsCallRequest toolsCallRequest) {
        try {
            String sceneKey = getRequiredArgument(toolsCallRequest, "sceneKey");
            this.logger.info2("sceneKey = " + sceneKey);

            SceneExecutionResult result = this.sceneService.executeBySceneKey(sceneKey);
            return successResponse(toolsCallRequest, result);

        } catch (Exception e) {
            logException("executeSceneToolResponse", e);
            return errorResponse(
                    toolsCallRequest,
                    "Failed to execute the specified SwitchBot scene.",
                    safeMessage(e)
            );
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // response helpers
    // -----------------------------------------------------------------------------------------------------------------

    private ToolsCallResponse successResponse(ToolsCallRequest toolsCallRequest, Object payload) throws Exception {
        ToolsCallResponse toolsCallResponse = baseToolCallResponse(toolsCallRequest);

        ToolsCallResponse.Content content = new ToolsCallResponse.Content();
        content.type = "text";
        content.text = mapper.writeValueAsString(payload);

        toolsCallResponse.result.addContent(content);
        return toolsCallResponse;
    }

    private ToolsCallResponse errorResponse(ToolsCallRequest toolsCallRequest, String message, String detail) {
        ToolsCallResponse toolsCallResponse = baseToolCallResponse(toolsCallRequest);
        toolsCallResponse.result.isError = true;

        ToolsCallResponse.Content content = new ToolsCallResponse.Content();
        content.type = "text";
        content.text = toErrorJson(message, detail);

        toolsCallResponse.result.addContent(content);
        return toolsCallResponse;
    }

    private ToolsCallResponse baseToolCallResponse(ToolsCallRequest toolsCallRequest) {
        ToolsCallResponse toolsCallResponse = new ToolsCallResponse();
        toolsCallResponse.jsonrpc = JSONRPC_VERSION;

        if (toolsCallRequest != null) {
            toolsCallResponse.id = toolsCallRequest.getId();
        }

        return toolsCallResponse;
    }

    private String toErrorJson(String message, String detail) {
        try {
            Map<String, Object> error = new LinkedHashMap<String, Object>();
            error.put("error", true);
            error.put("message", message);
            error.put("detail", detail);
            return mapper.writeValueAsString(error);
        } catch (Exception e) {
            return "{\"error\":true,\"message\":\"" + safeForJson(message) + "\"}";
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // request helpers
    // -----------------------------------------------------------------------------------------------------------------

    private String getToolName(ToolsCallRequest toolsCallRequest) {
        if (toolsCallRequest == null) {
            return null;
        }

        if (toolsCallRequest.getParams() == null) {
            return null;
        }

        return trimToNull(toolsCallRequest.getParams().getName());
    }

    private String getRequiredArgument(ToolsCallRequest toolsCallRequest, String argumentName) {
        String value = null;

        try {
            if (toolsCallRequest != null
                    && toolsCallRequest.getParams() != null
                    && toolsCallRequest.getParams().getArguments() != null) {
                value = toolsCallRequest.getParams().getArguments().getValue(argumentName);
            }
        } catch (Exception e) {
            value = null;
        }

        value = trimToNull(value);

        if (value == null) {
            throw new IllegalArgumentException("Missing required argument: " + argumentName);
        }

        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // logging helpers
    // -----------------------------------------------------------------------------------------------------------------

    private void logException(String context, Exception e) {
        this.logger.error_YELLOW(context);

        if (e == null) {
            this.logger.error_RED("Exception is null.");
            return;
        }

        this.logger.error_RED(e.toString());

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        printWriter.flush();

        this.logger.error_RED(stringWriter.toString());
    }

    private String safeMessage(Exception e) {
        if (e == null) {
            return "";
        }

        if (e.getMessage() == null || e.getMessage().trim().isEmpty()) {
            return e.toString();
        }

        return e.getMessage();
    }

    private String safeForJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
