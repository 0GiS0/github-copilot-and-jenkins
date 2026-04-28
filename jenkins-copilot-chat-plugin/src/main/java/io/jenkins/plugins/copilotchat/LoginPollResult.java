package io.jenkins.plugins.copilotchat;

public record LoginPollResult(
        String status, String error, String message, String login, Long id, int interval) {
    public static LoginPollResult pending(String message, int interval) {
        return new LoginPollResult("pending", null, message, null, null, interval);
    }

    public static LoginPollResult authenticated(String login, long id) {
        return new LoginPollResult("authenticated", null, null, login, id, 0);
    }

    public static LoginPollResult failed(String error, String message) {
        return new LoginPollResult("failed", error, message, null, null, 0);
    }
}
