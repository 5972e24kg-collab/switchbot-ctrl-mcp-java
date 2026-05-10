package vr46.switchbotctrlmcppublic.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import vr46.switchbotctrlmcppublic.logging.MyLogger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BaseMcpServlet extends HttpServlet {
    private static final ObjectMapper mapper = new ObjectMapper();
    protected MyLogger logger;
    protected static final String JSONRPC_VERSION = "2.0";
    private final String mcpProtocolVersion = "2025-06-18";
    public BaseMcpServlet(String version) {
        logger = new MyLogger(this.getClass().getSimpleName(), version);
    }

    //------------------------------------------------------------------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCorsHeaders(response);
        logger.info_CYAN("■ doPost");
        response.setCharacterEncoding("UTF-8");

        String mcpProtocolVersionHeader = trimToNull(request.getHeader("MCP-Protocol-Version"));
        String mcpSessionIdHeader = trimToNull(request.getHeader("Mcp-Session-Id"));
        String acceptHeader = trimToNull(request.getHeader("Accept"));
        String originHeader = trimToNull(request.getHeader("Origin"));
        String effectiveMcpProtocolVersion = (mcpProtocolVersionHeader != null) ? mcpProtocolVersionHeader : "not-specified";

        JsonNode root;
        try {
            root = mapper.readTree(request.getInputStream());
        } catch (Exception e) {
            logger.error_YELLOW("Parse error");
            logger.error_RED(e.getMessage());
            logger.error_RED(e.fillInStackTrace().getMessage());
            responseJson(response, 400, jsonRpcError((JsonNode) null, -32700, "Parse error", e.getMessage()));
            return;
        }

        if (!root.has("jsonrpc") || !JSONRPC_VERSION.equals(root.path("jsonrpc").asText())) {
            JsonNode id = root.has("id") ? root.get("id") : null;
            JsonNode jsonrpc = root.has("jsonrpc") ? root.path("jsonrpc") : null;
            String idText   = (id     != null) ? id.asText()     : "null";
            String rpcText  = (jsonrpc != null) ? jsonrpc.asText() : "null";
            logger.error_YELLOW("Invalid Request @ id = " + idText + " : jsonrpc = " + rpcText);
            responseJson(response, 400, jsonRpcError(id, -32600, "Invalid Request", "jsonrpc must be \"2.0\""));
            return;
        }

        String method = root.path("method").asText(null);
        JsonNode id = root.has("id") ? root.get("id") : null;
        if (method == null) {
            logger.error_YELLOW("Invalid Request @ method is required");
            responseJson(response, 400, jsonRpcError(id, -32600, "Invalid Request", "method is required"));
            return;
        }

        logger.info1("method=" + method + ", id=" + id);
        // MCP-Protocol-Version が明示されていて、かつ未対応なら拒否する。
        if (mcpProtocolVersionHeader != null && !mcpProtocolVersion.equals(mcpProtocolVersionHeader)) {  // ④ 参照箇所を更新
            logger.error_YELLOW("Unsupported MCP-Protocol-Version header: " + mcpProtocolVersionHeader  + ". Supported version is " + mcpProtocolVersion);
            responseJson(response, 400, jsonRpcError(id,-32000,"Unsupported protocol version"
                    ,"Unsupported MCP-Protocol-Version header: " + mcpProtocolVersionHeader  + ". Supported version is " + mcpProtocolVersion));
            return;
        }

        cleanupExpiredSessions();

        if ("initialize".equals(method)) {
            InitializeRequest initializeRequest = new InitializeRequest(root);
            logger.info_GREEN("initialize");
            doInitialize(response, initializeRequest);
            return;
        }

        McpSession session = requireSession(request, response, id, method);
        if (session == null) {
            return;
        }

        if ("notifications/initialized".equals(method)) {
            logger.info_GREEN("notifications/initialized");
            doNotificationsInitialized(response, session);
            return;
        }

        switch (method) {
            case "tools/list":
                ToolsListRequest toolsListRequest = new ToolsListRequest(root);  // ③ 変数名を小文字始まりに修正
                logger.info_GREEN("tools/list");
                doToolsList(response, toolsListRequest);
                return;

            case "tools/call":
                ToolsCallRequest toolsCallRequest = new ToolsCallRequest(root);
                //logger.info_GREEN("tools/call toolName = " + toolsCallRequest.getParams().getName());
                String toolName = "(missing)";
                if (toolsCallRequest.getParams() != null) {
                    toolName = toolsCallRequest.getParams().getName();
                }
                logger.info_GREEN("tools/call toolName = " + toolName);
                doToolsCall(response, toolsCallRequest);
                return;

            default:
                logger.error_YELLOW("Method not found: " + method);
                responseJson(response, 200, jsonRpcError(id, -32601, "Method not found", method));
        }

    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        addCorsHeaders(response);
        logger.info_CYAN("■ doGet Accept=" + request.getHeader("Accept"));
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED); // 405
        response.setContentType("text/plain; charset=UTF-8");
        response.getWriter().write("SSE stream is not supported in this minimal implementation.");
    }
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        addCorsHeaders(response);
        logger.info_CYAN("■ doOptions");
        response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
    }
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        addCorsHeaders(response);

        String sessionId = trimToNull(request.getHeader("Mcp-Session-Id"));
        if (sessionId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("Mcp-Session-Id is required.");
            return;
        }

        McpSession removed = sessions.remove(sessionId);
        if (removed == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("MCP session not found.");
            return;
        }

        logger.info_CYAN("■ doDelete　session deleted: " + sessionId);;
        response.setStatus(HttpServletResponse.SC_ACCEPTED); // 202
    }

    //------------------------------------------------------------------------------------------------------------------
    //ACL
    private void addCorsHeaders(HttpServletResponse response) {
        // このサンプルでは、ローカル環境での検証用に制限の緩い（パーミッシブな）CORS設定を使用しています。
        // 本番環境で使用する場合は、Access-Control-Allow-Origin を、お使いの Open WebUI のオリジンなど、信頼できる MCP クライアントのみに制限してください。
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization, MCP-Protocol-Version, Mcp-Session-Id");
        response.setHeader("Access-Control-Expose-Headers", "Mcp-Session-Id");
    }

    //------------------------------------------------------------------------------------------------------------------
    //セッション管理
    private final ConcurrentHashMap<String, McpSession> sessions = new ConcurrentHashMap<>();

    private static class McpSession {
        private String sessionId;
        private String protocolVersion;
        private String clientName;
        private String clientVersion;
        private volatile boolean initialized;
        private long createdAtMillis;
        private volatile long lastAccessAtMillis;
    }
    private static final long SESSION_TTL_MILLIS = 60L * 60L * 1000L; // 1時間

    private void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();

        sessions.entrySet().removeIf(entry ->
                now - entry.getValue().lastAccessAtMillis > SESSION_TTL_MILLIS
        );
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
    private String newSessionId() {
        return UUID.randomUUID().toString();
    }

    private McpSession createSession(InitializeRequest initializeRequest) {
        long now = System.currentTimeMillis();

        InitializeRequest.Params      params     = initializeRequest.getParams();
        InitializeRequest.ClientInfo  clientInfo = (params != null) ? params.getClientInfo() : null;

        McpSession session = new McpSession();
        session.sessionId      = newSessionId();
        session.protocolVersion = this.mcpProtocolVersion;  // ④ 参照箇所を更新
        session.clientName    = (clientInfo != null && clientInfo.getName()    != null) ? clientInfo.getName()    : "unknown-client";
        session.clientVersion = (clientInfo != null && clientInfo.getVersion() != null) ? clientInfo.getVersion() : "unknown-version";
        session.initialized = false;
        session.createdAtMillis = now;
        session.lastAccessAtMillis = now;

        sessions.put(session.sessionId, session);

        logger.info1("session created: " + session.sessionId
                + ", clientName=" + session.clientName
                + ", clientVersion=" + session.clientVersion);

        return session;
    }

    private static final boolean REQUIRE_INITIALIZED_NOTIFICATION = false;

    private McpSession requireSession(
            HttpServletRequest request,
            HttpServletResponse response,
            JsonNode id,
            String method
    ) throws IOException {

        String sessionId = trimToNull(request.getHeader("Mcp-Session-Id"));

        if (sessionId == null) {
            logger.error_YELLOW("Missing Mcp-Session-Id");
            responseJson(response, 400, jsonRpcError(id,-32000,
                    "Missing Mcp-Session-Id","This MCP server requires Mcp-Session-Id after initialize."));
            return null;
        }

        McpSession session = sessions.get(sessionId);

        if (session == null) {
            logger.error_YELLOW("Unknown MCP session");
            responseJson(response,404, jsonRpcError(id,-32001,
                    "Unknown MCP session","Session not found or already expired. Please initialize again."));
            return null;
        }

        long now = System.currentTimeMillis();

        if (now - session.lastAccessAtMillis > SESSION_TTL_MILLIS) {
            sessions.remove(sessionId);
            logger.error_YELLOW("Expired MCP session");
            responseJson(response,404, jsonRpcError(id,-32001,
                    "Expired MCP session","Session expired. Please initialize again."));
            return null;
        }

        session.lastAccessAtMillis = now;

        if ("notifications/initialized".equals(method)) {
            return session;
        }

        if (!session.initialized) {
            if (REQUIRE_INITIALIZED_NOTIFICATION) {
                logger.error_YELLOW("MCP session is not initialized");
                responseJson(response,400, jsonRpcError(id,-32002,
                        "MCP session is not initialized","Send notifications/initialized before calling MCP tools."));
                return null;
            }
            session.initialized = true;
        }

        return session;
    }

    //------------------------------------------------------------------------------------------------------------------
    //固定の戻り
    private void responseJson(HttpServletResponse response, int status, JsonNode body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        mapper.writeValue(response.getWriter(), body);
    }
    private ObjectNode jsonRpcError(JsonNode id, int code, String message, String data) {
        Integer intId = (id != null && id.isNumber()) ? id.asInt() : null;
        return jsonRpcError(intId, code, message, data);
    }
    protected ObjectNode jsonRpcError(Integer id, int code, String message, String data) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", JSONRPC_VERSION);
        if (id != null) {
            response.put("id", id);
        } else {
            response.putNull("id");
        }

        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.put("data", data);
        }

        response.set("error", error);
        return response;
    }

    //------------------------------------------------------------------------------------------------------------------
    protected void doInitialize(HttpServletResponse response, InitializeRequest initializeRequest) throws IOException {
        McpSession session = createSession(initializeRequest);
        response.setHeader("Mcp-Session-Id", session.sessionId);
        InitializeResponse initializeResponse = resInitialize(initializeRequest);
        ObjectNode responseNode = mapper.convertValue(initializeResponse, ObjectNode.class);
        responseJson(response, 200, responseNode);
    }
    protected InitializeResponse resInitialize(InitializeRequest initializeRequest) {
        InitializeResponse initializeResponse = new InitializeResponse();
        initializeResponse.jsonrpc = this.JSONRPC_VERSION;
        initializeResponse.id = initializeRequest.getId();
        initializeResponse.result.protocolVersion = this.mcpProtocolVersion;  // ④ 参照箇所を更新
        return initializeResponse;
    }

    //------------------------------------------------------------------------------------------------------------------
    protected void doNotificationsInitialized(HttpServletResponse response, McpSession session) {
        session.initialized = true;
        session.lastAccessAtMillis = System.currentTimeMillis();
        response.setStatus(HttpServletResponse.SC_ACCEPTED); // 202
    }

    //------------------------------------------------------------------------------------------------------------------
    protected void doToolsList(HttpServletResponse response, ToolsListRequest toolsListRequest) throws IOException {
        ToolsListResponse toolsListResponse = resToolsList(toolsListRequest);
        ObjectNode responseNode = mapper.convertValue(toolsListResponse, ObjectNode.class);
        responseJson(response, 200, responseNode);
    }
    protected ToolsListResponse resToolsList(ToolsListRequest toolsListRequest) {
        ToolsListResponse toolsListResponse = new ToolsListResponse();
        toolsListResponse.jsonrpc = this.JSONRPC_VERSION;
        toolsListResponse.id = toolsListRequest.getId();

        return toolsListResponse;
    }

    //------------------------------------------------------------------------------------------------------------------
    protected void doToolsCall(HttpServletResponse response, ToolsCallRequest toolsCallRequest) throws IOException {
        ToolsCallResponse toolsCallResponse = resToolsCall(toolsCallRequest);
        if(toolsCallResponse == null) {
            responseJson(response, 200, jsonRpcError(toolsCallRequest.getId(), -32601, "Unknown tool", "Tool not found: " + toolsCallRequest.getParams().getName()));  // ⑥ -32602 → -32601
        } else {
            ObjectNode responseNode = mapper.convertValue(toolsCallResponse, ObjectNode.class);
            responseJson(response, 200, responseNode);
        }
    }

    protected ToolsCallResponse resToolsCall(ToolsCallRequest toolsCallRequest) {
        return null;    //ツール無し（サブクラスでオーバーライドして実装する）
    }
}