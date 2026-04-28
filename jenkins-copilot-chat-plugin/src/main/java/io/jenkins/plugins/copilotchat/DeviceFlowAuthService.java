package io.jenkins.plugins.copilotchat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.User;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceFlowAuthService {
    private static final URI DEVICE_CODE_URI = URI.create("https://github.com/login/device/code");
    private static final URI ACCESS_TOKEN_URI = URI.create("https://github.com/login/oauth/access_token");
    private static final URI USER_URI = URI.create("https://api.github.com/user");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final GitHubTokenStore tokenStore;
    private final Map<String, PendingLogin> pendingLogins = new ConcurrentHashMap<>();

    public DeviceFlowAuthService(GitHubTokenStore tokenStore) {
        this(HttpClient.newHttpClient(), new ObjectMapper(), Clock.systemUTC(), tokenStore);
    }

    DeviceFlowAuthService(HttpClient httpClient, ObjectMapper objectMapper, Clock clock, GitHubTokenStore tokenStore) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.tokenStore = tokenStore;
    }

    public LoginStart startLogin(User user, CopilotChatConfiguration configuration) throws IOException, InterruptedException {
        requireClientId(configuration);

        String body = form(Map.of(
                "client_id", configuration.getClientId(),
                "scope", "read:user"));
        HttpRequest request = HttpRequest.newBuilder(DEVICE_CODE_URI)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("GitHub device code request failed with HTTP " + response.statusCode());
        }

        JsonNode json = objectMapper.readTree(response.body());
        DeviceCodeResponse deviceCode = new DeviceCodeResponse(
                requiredText(json, "device_code"),
                requiredText(json, "user_code"),
                requiredText(json, "verification_uri"),
                json.path("expires_in").asInt(900),
                json.path("interval").asInt(5));
        String loginId = UUID.randomUUID().toString();
        pendingLogins.put(loginId, new PendingLogin(
                user.getId(),
                deviceCode.deviceCode(),
                Instant.now(clock).plusSeconds(deviceCode.expiresIn()),
                Math.max(1, deviceCode.interval())));
        return new LoginStart(loginId, deviceCode.userCode(), deviceCode.verificationUri(), deviceCode.expiresIn(), deviceCode.interval());
    }

    public LoginPollResult pollLogin(User user, String loginId, CopilotChatConfiguration configuration) throws IOException, InterruptedException {
        requireClientId(configuration);
        PendingLogin pending = pendingLogins.get(loginId);
        if (pending == null || !pending.jenkinsUserId().equals(user.getId())) {
            return LoginPollResult.failed("not_found", "Login request was not found.");
        }
        if (Instant.now(clock).isAfter(pending.expiresAt())) {
            pendingLogins.remove(loginId);
            return LoginPollResult.failed("expired_token", "The device code expired.");
        }

        String body = form(Map.of(
                "client_id", configuration.getClientId(),
                "device_code", pending.deviceCode(),
                "grant_type", "urn:ietf:params:oauth:grant-type:device_code"));
        HttpRequest request = HttpRequest.newBuilder(ACCESS_TOKEN_URI)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(response.body());
        if (json.hasNonNull("error")) {
            String error = json.path("error").asText();
            if ("authorization_pending".equals(error) || "slow_down".equals(error)) {
                return LoginPollResult.pending(error, pending.intervalSeconds() + ("slow_down".equals(error) ? 5 : 0));
            }
            if ("expired_token".equals(error) || "access_denied".equals(error)) {
                pendingLogins.remove(loginId);
            }
            return LoginPollResult.failed(error, json.path("error_description").asText(error));
        }

        String accessToken = requiredText(json, "access_token");
        GitHubIdentity identity = fetchIdentity(accessToken);
        tokenStore.save(user, accessToken, identity.login(), identity.id());
        pendingLogins.remove(loginId);
        return LoginPollResult.authenticated(identity.login(), identity.id());
    }

    public Optional<GitHubIdentity> getStoredIdentity(User user) {
        return tokenStore.getIdentity(user);
    }

    public void logout(User user) {
        tokenStore.delete(user);
    }

    private GitHubIdentity fetchIdentity(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(USER_URI)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("GitHub identity request failed with HTTP " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        return new GitHubIdentity(requiredText(json, "login"), json.path("id").asLong());
    }

    private static void requireClientId(CopilotChatConfiguration configuration) {
        if (configuration == null || !configuration.isConfigured()) {
            throw new IllegalStateException("Copilot Chat OAuth App client ID is not configured.");
        }
    }

    private static String requiredText(JsonNode json, String field) throws IOException {
        String value = json.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IOException("GitHub response did not include " + field);
        }
        return value;
    }

    private static String form(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        values.forEach((key, value) -> {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            builder.append('=');
            builder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return builder.toString();
    }

    private record PendingLogin(String jenkinsUserId, String deviceCode, Instant expiresAt, int intervalSeconds) {}
}