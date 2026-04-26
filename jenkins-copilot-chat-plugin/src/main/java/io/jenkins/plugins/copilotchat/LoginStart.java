package io.jenkins.plugins.copilotchat;

public record LoginStart(String loginId, String userCode, String verificationUri, int expiresIn, int interval) {}