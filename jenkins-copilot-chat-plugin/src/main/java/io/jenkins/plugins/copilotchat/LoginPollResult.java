package io.jenkins.plugins.copilotchat;

/**
 * 🔄 Represents the result of a single Device Flow polling attempt.
 *
 * <p>The browser polls the {@code /copilot-chat/pollLogin} endpoint repeatedly while the user
 * completes the authorization on GitHub. Each poll returns one of three states:
 *
 * <ul>
 *   <li>⏳ <b>pending</b> — the user hasn't authorized yet; keep polling after {@code interval}
 *       seconds.
 *   <li>✅ <b>authenticated</b> — authorization succeeded; {@code login} and {@code id} are set.
 *   <li>❌ <b>failed</b> — authorization failed or expired; {@code error} describes the reason.
 * </ul>
 *
 * <p>The static factory methods make constructing each variant readable and self-documenting.
 */
public record LoginPollResult(
        String status, String error, String message, String login, Long id, int interval) {

    /** ⏳ Returns a result indicating that the user hasn't yet approved the request. */
    public static LoginPollResult pending(String message, int interval) {
        return new LoginPollResult("pending", null, message, null, null, interval);
    }

    /** ✅ Returns a result indicating successful authentication. */
    public static LoginPollResult authenticated(String login, long id) {
        return new LoginPollResult("authenticated", null, null, login, id, 0);
    }

    /** ❌ Returns a result indicating that the login flow failed or was denied. */
    public static LoginPollResult failed(String error, String message) {
        return new LoginPollResult("failed", error, message, null, null, 0);
    }
}
