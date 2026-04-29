package io.jenkins.plugins.copilotchat;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.CopilotClientOptions;
import hudson.model.User;
import java.util.Optional;

public class CopilotClientFactory {
    private final GitHubTokenStore tokenStore;

    public CopilotClientFactory(GitHubTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public CopilotClient createFor(User user, CopilotChatConfiguration configuration) {
        Optional<String> token = tokenStore.getToken(user);
        if (token.isEmpty()) {
            throw new IllegalStateException("User is not authenticated with GitHub.");
        }
        CopilotClientOptions options = new CopilotClientOptions();
        if (configuration.getCliUrl() != null && !configuration.getCliUrl().isBlank()) {
            // Remote CLI server manages its own auth; token/useLoggedInUser are not allowed
            options.setCliUrl(configuration.getCliUrl());
        } else {
            options.setGitHubToken(token.get()).setUseLoggedInUser(false);
            if (configuration.getCliPath() != null && !configuration.getCliPath().isBlank()) {
                options.setCliPath(configuration.getCliPath());
            }
        }
        return new CopilotClient(options);
    }
}
