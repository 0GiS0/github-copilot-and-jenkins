package io.jenkins.plugins.copilotchat;

public record MessageRequest(String prompt, String pagePath, String model) {}