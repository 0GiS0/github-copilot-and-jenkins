package io.jenkins.plugins.copilotchat;

public record DeviceCodeResponse(
        String deviceCode,
        String userCode,
        String verificationUri,
        int expiresIn,
        int interval) {}