package vr46.switchbotctrlmcppublic.switchbot;

import java.util.Objects;

public record SwitchBotApiResult(
        int statusCode,
        String body,
        boolean success,
        String method,
        String path
) {
    public SwitchBotApiResult {
        body = body == null ? "" : body;
        success = statusCode >= 200 && statusCode < 300;
        method = requireNonBlank(method, "method");
        path = requireNonBlank(path, "path");
    }

    public SwitchBotApiResult(int statusCode, String body, String method, String path) {
        this(statusCode, body, statusCode >= 200 && statusCode < 300, method, path);
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null.");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }
}
