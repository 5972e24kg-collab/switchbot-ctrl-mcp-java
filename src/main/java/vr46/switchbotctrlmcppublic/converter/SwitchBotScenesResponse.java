package vr46.switchbotctrlmcppublic.converter;

import java.util.List;

public record SwitchBotScenesResponse(
        Integer statusCode,
        List<Scene> body,
        String message
) {
    public SwitchBotScenesResponse {
        body = body == null ? null : List.copyOf(body);
    }

    public List<Scene> bodyOrEmpty() {
        return body == null ? List.of() : body;
    }

    public record Scene(String sceneId, String sceneName) {
    }
}