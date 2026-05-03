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

/**
 * 🔐 Implements the GitHub OAuth 2.0 Device Authorization Flow.
 *
 * <h2>What is the Device Flow?</h2>
 *
 * <p>The Device Flow is an OAuth 2.0 grant type designed for devices that cannot open a browser
 * (e.g. CLIs, TVs, IoT devices). The flow works in three steps:
 *
 * <ol>
 *   <li>📲 <b>Start</b> — the server asks GitHub for a short user code and a verification URL. The
 *       user is told to visit the URL and type the code.
 *   <li>🔄 <b>Poll</b> — while the user is authorizing, the server polls GitHub every few seconds
 *       asking "has the user approved yet?".
 *   <li>✅ <b>Success</b> — once approved, GitHub returns an access token. The plugin stores it in
 *       {@link GitHubTokenStore} tied to the Jenkins user.
 * </ol>
 *
 * <h2>State management</h2>
 *
 * <p>In-flight logins are tracked in {@link #pendingLogins}, keyed by a random {@code loginId}
 * generated at step 1. This map is in-memory only; a Jenkins restart clears all pending logins.
 */
public class DeviceFlowAuthService {
    // 🌐 GitHub API endpoints for the Device Flow
    private static final URI DEVICE_CODE_URI = URI.create("https://github.com/login/device/code");
    private static final URI ACCESS_TOKEN_URI =
            URI.create("https://github.com/login/oauth/access_token");
    private static final URI USER_URI = URI.create("https://api.github.com/user");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final GitHubTokenStore tokenStore;
    // 📝 Pending logins awaiting user authorization, keyed by the random loginId
    private final Map<String, PendingLogin> pendingLogins = new ConcurrentHashMap<>();

    public DeviceFlowAuthService(GitHubTokenStore tokenStore) {
        this(HttpClient.newHttpClient(), new ObjectMapper(), Clock.systemUTC(), tokenStore);
    }

    /** 🧪 Package-private constructor for unit testing with mock dependencies. */
    DeviceFlowAuthService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            Clock clock,
            GitHubTokenStore tokenStore) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.tokenStore = tokenStore;
    }

    /**
     * 📲 Step 1 of the Device Flow: request a device code from GitHub.
     *
     * <p>This method:
     *
     * <ol>
     *   <li>Makes a POST to {@code /login/device/code} with the configured OAuth App client ID.
     *   <li>GitHub responds with a short user code, a verification URL, and a device code.
     *   <li>Stores the in-progress state in {@link #pendingLogins} under a random {@code loginId}.
     *   <li>Returns a {@link LoginStart} with everything the browser needs to show the login UI.
     * </ol>
     *
     * @throws IllegalStateException if the plugin is not configured (client ID missing)
     * @throws IOException if the HTTP request to GitHub fails
     */
    public LoginStart startLogin(User user, CopilotChatConfiguration configuration)
            throws IOException, InterruptedException {
        requireClientId(configuration);

        // 📤 POST to GitHub to get a device code and user code
        String body = form(Map.of("client_id", configuration.getClientId(), "scope", "read:user"));
        HttpRequest request =
                HttpRequest.newBuilder(DEVICE_CODE_URI)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(
                    "GitHub device code request failed with HTTP " + response.statusCode());
        }

        // 📝 Parse the response and build the DeviceCodeResponse record
        JsonNode json = objectMapper.readTree(response.body());
        DeviceCodeResponse deviceCode =
                new DeviceCodeResponse(
                        requiredText(json, "device_code"),
                        requiredText(json, "user_code"),
                        requiredText(json, "verification_uri"),
                        json.path("expires_in").asInt(900),
                        json.path("interval").asInt(5));

        // 🔑 Store the pending login state so pollLogin() can retrieve it later
        String loginId = UUID.randomUUID().toString();
        pendingLogins.put(
                loginId,
                new PendingLogin(
                        user.getId(),
                        deviceCode.deviceCode(),
                        Instant.now(clock).plusSeconds(deviceCode.expiresIn()),
                        Math.max(1, deviceCode.interval())));
        return new LoginStart(
                loginId,
                deviceCode.userCode(),
                deviceCode.verificationUri(),
                deviceCode.expiresIn(),
                deviceCode.interval());
    }

    /**
     * 🔄 Step 2 of the Device Flow: poll GitHub to check if the user has authorized.
     *
     * <p>The browser calls this endpoint repeatedly (every {@code interval} seconds) after
     * displaying the user code. Possible outcomes:
     *
     * <ul>
     *   <li>⏳ {@code authorization_pending} / {@code slow_down} — user hasn't approved yet; return
     *       a {@link LoginPollResult#pending pending} result with the suggested interval.
     *   <li>✅ Access token received — fetch the GitHub identity, store the token, and return {@link
     *       LoginPollResult#authenticated authenticated}.
     *   <li>❌ Any other error (expired, denied, etc.) — remove the pending entry and return {@link
     *       LoginPollResult#failed failed}.
     * </ul>
     */
    public LoginPollResult pollLogin(
            User user, String loginId, CopilotChatConfiguration configuration)
            throws IOException, InterruptedException {
        requireClientId(configuration);
        PendingLogin pending = pendingLogins.get(loginId);
        // 🔒 Guard: only the user who started the login can poll it
        if (pending == null || !pending.jenkinsUserId().equals(user.getId())) {
            return LoginPollResult.failed("not_found", "Login request was not found.");
        }
        // ⏰ Guard: reject expired device codes immediately without making a network call
        if (Instant.now(clock).isAfter(pending.expiresAt())) {
            pendingLogins.remove(loginId);
            return LoginPollResult.failed("expired_token", "The device code expired.");
        }

        // 📤 POST to GitHub to exchange the device code for an access token
        String body =
                form(
                        Map.of(
                                "client_id", configuration.getClientId(),
                                "device_code", pending.deviceCode(),
                                "grant_type", "urn:ietf:params:oauth:grant-type:device_code"));
        HttpRequest request =
                HttpRequest.newBuilder(ACCESS_TOKEN_URI)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(response.body());
        if (json.hasNonNull("error")) {
            String error = json.path("error").asText();
            // ⏳ These two codes mean "keep polling" — the user hasn't acted yet
            if ("authorization_pending".equals(error) || "slow_down".equals(error)) {
                return LoginPollResult.pending(
                        error, pending.intervalSeconds() + ("slow_down".equals(error) ? 5 : 0));
            }
            // ❌ Terminal errors: clean up the pending entry
            if ("expired_token".equals(error) || "access_denied".equals(error)) {
                pendingLogins.remove(loginId);
            }
            return LoginPollResult.failed(error, json.path("error_description").asText(error));
        }

        // ✅ Success! Exchange complete — fetch the GitHub identity and persist the token
        String accessToken = requiredText(json, "access_token");
        GitHubIdentity identity = fetchIdentity(accessToken);
        tokenStore.save(user, accessToken, identity.login(), identity.id());
        pendingLogins.remove(loginId);
        return LoginPollResult.authenticated(identity.login(), identity.id());
    }

    /** 👤 Returns the stored GitHub identity for a user, if they have previously logged in. */
    public Optional<GitHubIdentity> getStoredIdentity(User user) {
        return tokenStore.getIdentity(user);
    }

    /** 🚪 Removes the user's stored token, effectively logging them out. */
    public void logout(User user) {
        tokenStore.delete(user);
    }

    /**
     * 👤 Calls {@code GET /user} to retrieve the authenticated user's GitHub profile. This is the
     * final step after receiving an access token — it resolves the login name and numeric ID so
     * they can be stored alongside the token.
     */
    private GitHubIdentity fetchIdentity(String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder(USER_URI)
                        .header("Accept", "application/vnd.github+json")
                        .header("Authorization", "Bearer " + accessToken)
                        .GET()
                        .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(
                    "GitHub identity request failed with HTTP " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        return new GitHubIdentity(requiredText(json, "login"), json.path("id").asLong());
    }

    /**
     * ⚠️ Validates that the plugin is configured before making any OAuth request. Throws early with
     * a meaningful message rather than failing later with a NullPointerException.
     */
    private static void requireClientId(CopilotChatConfiguration configuration) {
        if (configuration == null || !configuration.isConfigured()) {
            throw new IllegalStateException("Copilot Chat OAuth App client ID is not configured.");
        }
    }

    /**
     * 🔍 Extracts a required text field from a JSON node. Throws {@link IOException} with a helpful
     * message if the field is absent.
     */
    private static String requiredText(JsonNode json, String field) throws IOException {
        String value = json.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IOException("GitHub response did not include " + field);
        }
        return value;
    }

    /**
     * 📝 Encodes a map of key-value pairs as an {@code application/x-www-form-urlencoded} string.
     * Used to build POST bodies for the GitHub OAuth endpoints.
     */
    private static String form(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        values.forEach(
                (key, value) -> {
                    if (builder.length() > 0) {
                        builder.append('&');
                    }
                    builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
                    builder.append('=');
                    builder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                });
        return builder.toString();
    }

    /**
     * 📄 Immutable value object holding the state of an in-progress Device Flow login. Stored in
     * {@link #pendingLogins} between the start and completion of the flow.
     */
    private record PendingLogin(
            String jenkinsUserId, String deviceCode, Instant expiresAt, int intervalSeconds) {}
}
