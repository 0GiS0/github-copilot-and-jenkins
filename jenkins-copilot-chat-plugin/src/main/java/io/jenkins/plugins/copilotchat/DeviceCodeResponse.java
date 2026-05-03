package io.jenkins.plugins.copilotchat;

/**
 * 📲 Represents the response from GitHub's Device Code endpoint.
 *
 * <p>During the OAuth Device Flow, the first HTTP call is made to
 * {@code https://github.com/login/device/code}. GitHub responds with the fields captured here:
 *
 * <ul>
 *   <li>{@code deviceCode} — an opaque code the server uses internally to track the authorization.</li>
 *   <li>{@code userCode} — a short, human-readable code (e.g. {@code ABCD-1234}) that the user
 *       must type on GitHub's verification page to grant access.</li>
 *   <li>{@code verificationUri} — the URL the user must visit (usually {@code https://github.com/login/device}).</li>
 *   <li>{@code expiresIn} — seconds until both {@code deviceCode} and {@code userCode} expire.</li>
 *   <li>{@code interval} — minimum seconds between polling requests to avoid rate limiting.</li>
 * </ul>
 *
 * <p>This is a Java 16+ {@code record} — an immutable data carrier with auto-generated
 * constructor, getters, {@code equals}, {@code hashCode}, and {@code toString}.
 */
public record DeviceCodeResponse(
        String deviceCode, String userCode, String verificationUri, int expiresIn, int interval) {}
