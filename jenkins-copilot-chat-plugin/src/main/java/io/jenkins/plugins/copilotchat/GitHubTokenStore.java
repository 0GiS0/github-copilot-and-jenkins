package io.jenkins.plugins.copilotchat;

import hudson.model.User;
import hudson.util.Secret;
import java.io.IOException;
import java.util.Optional;

public class GitHubTokenStore {
    private static final String TOKEN_PROPERTY_NAME = CopilotTokenUserProperty.class.getName();

    public void save(User user, String token, String login, long githubUserId) throws IOException {
        user.addProperty(new CopilotTokenUserProperty(Secret.fromString(token), login, githubUserId));
        user.save();
    }

    public Optional<String> getToken(User user) {
        CopilotTokenUserProperty property = getProperty(user);
        if (property == null || property.getToken() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Secret.toString(property.getToken()));
    }

    public Optional<GitHubIdentity> getIdentity(User user) {
        CopilotTokenUserProperty property = getProperty(user);
        if (property == null || property.getGithubLogin() == null) {
            return Optional.empty();
        }
        return Optional.of(new GitHubIdentity(property.getGithubLogin(), property.getGithubUserId()));
    }

    public void delete(User user) {
        try {
            user.getProperties().remove(TOKEN_PROPERTY_NAME);
            user.save();
        } catch (IOException e) {
            throw new IllegalStateException("Could not remove stored GitHub token", e);
        }
    }

    private CopilotTokenUserProperty getProperty(User user) {
        return user.getProperty(CopilotTokenUserProperty.class);
    }
}