package vr46.switchbotctrlmcppublic.scene;

public record SceneExecutionResult(
        String sceneKey,
        String sceneId,
        String sceneName,
        boolean success,
        int httpStatusCode,
        String switchBotBody,
        String message,
        boolean executed,
        boolean dryRun
) {
    public SceneExecutionResult {
        switchBotBody = switchBotBody == null ? "" : switchBotBody;
        message = message == null ? "" : message;
    }
}
