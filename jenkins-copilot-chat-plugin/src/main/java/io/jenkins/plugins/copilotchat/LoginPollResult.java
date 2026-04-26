package io.jenkins.plugins.copilotchat;

public record LoginPollResult(String status, String error, String message, String login, int interval) {
    public static LoginPollResult pending(String message, int interval) {
        return new LoginPollResult("pending", null, message, null, interval);
    }

    public static LoginPollResult authenticated(String login) {
        return new LoginPollResult("authenticated", null, null, login, 0);
    }

    public static LoginPollResult failed(String error, String message) {
        return new LoginPollResult("failed", error, message, null, 0);
    }
}