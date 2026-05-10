package vr46.switchbotctrlmcppublic.config;

public class AppConfig {
    public final String TOKEN;
    public final String SECRET;

    private AppConfig() {
        this.TOKEN = requireEnv("TOKEN");
        this.SECRET = requireEnv("SECRET");
    }

    public static AppConfig load() {
        return new AppConfig();
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null) {
            throw new IllegalStateException("Required environment variable is missing: " + name);
        }
        return value;
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null ? defaultValue : value;
    }
}