package io.jenkins.plugins.copilotchat;

/**
 * 🚀 Carries the data the browser needs to display the GitHub Device Flow login prompt.
 *
 * <p>When the user clicks "Login with GitHub", the server calls {@link
 * DeviceFlowAuthService#startLogin} which returns a {@code LoginStart} instance. This object is
 * serialized to JSON and sent back to the browser, which then:
 *
 * <ol>
 *   <li>Shows {@code userCode} to the user (e.g. {@code ABCD-1234}).
 *   <li>Opens (or prompts the user to open) {@code verificationUri} in their browser.
 *   <li>Starts polling {@code /copilot-chat/pollLogin?loginId=...} every {@code interval} seconds.
 * </ol>
 *
 * <ul>
 *   <li>{@code loginId} — server-side key used to look up the pending login during polling.
 *   <li>{@code userCode} — the short code the user types on GitHub.
 *   <li>{@code verificationUri} — the GitHub URL to visit.
 *   <li>{@code expiresIn} — seconds until the code expires.
 *   <li>{@code interval} — recommended polling interval in seconds.
 * </ul>
 */
public record LoginStart(
        String loginId, String userCode, String verificationUri, int expiresIn, int interval) {}
