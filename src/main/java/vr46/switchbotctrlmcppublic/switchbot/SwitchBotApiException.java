package vr46.switchbotctrlmcppublic.switchbot;

public class SwitchBotApiException extends RuntimeException {
    public SwitchBotApiException(String message) {
        super(message);
    }

    public SwitchBotApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
