package vr46.switchbotctrlmcppublic.switchbot;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class SwitchBotApisV2 {
    private static final String DEFAULT_HOST = "https://api.switch-bot.com";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private static final String DEVICES_PATH = "/v1.1/devices";
    private static final String SCENES_PATH = "/v1.1/scenes";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private final String token;
    private final String secret;
    private final String host;
    private final Duration requestTimeout;
    private final HttpClient httpClient;

    public SwitchBotApisV2(String token, String secret) {
        this(token, secret, DEFAULT_HOST, DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT);
    }

    public SwitchBotApisV2(String token, String secret, String host) {
        this(token, secret, host, DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT);
    }

    public SwitchBotApisV2(
            String token,
            String secret,
            String host,
            Duration connectTimeout,
            Duration requestTimeout
    ) {
        this(
                token,
                secret,
                host,
                requestTimeout,
                HttpClient.newBuilder()
                        .connectTimeout(requirePositive(connectTimeout, "connectTimeout"))
                        .build()
        );
    }

    public SwitchBotApisV2(
            String token,
            String secret,
            String host,
            Duration requestTimeout,
            HttpClient httpClient
    ) {
        this.token = requireNonBlank(token, "token");
        this.secret = requireNonBlank(secret, "secret");
        this.host = normalizeHost(host);
        this.requestTimeout = requirePositive(requestTimeout, "requestTimeout");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null.");
    }

    public SwitchBotApiResult getDevices() {
        return doGet(DEVICES_PATH);
    }

    public SwitchBotApiResult getScenes() {
        return doGet(SCENES_PATH);
    }

    public SwitchBotApiResult executeScene(String sceneId) {
        String encodedSceneId = encodePathSegment(requireNonBlank(sceneId, "sceneId"));
        return doPost(SCENES_PATH + "/" + encodedSceneId + "/execute");
    }

    private SwitchBotApiResult doGet(String path) {
        return send("GET", path);
    }

    private SwitchBotApiResult doPost(String path) {
        return send("POST", path);
    }

    private SwitchBotApiResult send(String method, String path) {
        URI uri = buildUri(path);
        AuthHeaders authHeaders = createAuthHeaders();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Authorization", token)
                .header("sign", authHeaders.sign())
                .header("nonce", authHeaders.nonce())
                .header("t", authHeaders.timestamp());

        if ("GET".equals(method)) {
            requestBuilder.GET();
        } else if ("POST".equals(method)) {
            requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return new SwitchBotApiResult(response.statusCode(), response.body(), method, path);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SwitchBotApiException("SwitchBot API request was interrupted.", e);
        } catch (IOException e) {
            throw new SwitchBotApiException("Failed to complete SwitchBot API request.", e);
        }
    }

    private URI buildUri(String path) {
        try {
            return URI.create(host + path);
        } catch (IllegalArgumentException e) {
            throw new SwitchBotApiException("Failed to build SwitchBot API URI.", e);
        }
    }

    private AuthHeaders createAuthHeaders() {
        String nonce = UUID.randomUUID().toString();
        String timestamp = Long.toString(Instant.now().toEpochMilli());

        try {
            return new AuthHeaders(sign(timestamp, nonce), nonce, timestamp);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SwitchBotApiException("Failed to sign SwitchBot API request.", e);
        }
    }

    private String sign(String timestamp, String nonce) throws NoSuchAlgorithmException, InvalidKeyException {
        String data = token + timestamp + nonce;
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(secretKeySpec);
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static String normalizeHost(String host) {
        String normalized = requireNonBlank(host, "host").trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("host must not be blank.");
        }

        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("host must be a valid absolute URI.", e);
        }

        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("host must be an absolute URI with scheme and host.");
        }
        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("host must not include query or fragment.");
        }
        return normalized;
    }

    private static Duration requirePositive(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " must not be null.");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be greater than zero.");
        }
        return duration;
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null.");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }

    private static String encodePathSegment(String value) {
        StringBuilder encoded = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (Character.isISOControl(codePoint)) {
                throw new IllegalArgumentException("sceneId must not contain control characters.");
            }

            if (isUnreserved(codePoint)) {
                encoded.appendCodePoint(codePoint);
            } else {
                // Reserved characters such as '/' are percent-encoded so sceneId stays one path segment.
                byte[] bytes = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    encoded.append('%');
                    encoded.append(HEX[(b >> 4) & 0x0F]);
                    encoded.append(HEX[b & 0x0F]);
                }
            }
            i += Character.charCount(codePoint);
        }
        return encoded.toString();
    }

    private static boolean isUnreserved(int codePoint) {
        return (codePoint >= 'A' && codePoint <= 'Z')
                || (codePoint >= 'a' && codePoint <= 'z')
                || (codePoint >= '0' && codePoint <= '9')
                || codePoint == '-'
                || codePoint == '.'
                || codePoint == '_'
                || codePoint == '~';
    }

    private record AuthHeaders(String sign, String nonce, String timestamp) {
    }
}
