package io.jenkins.plugins.copilotchat;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.CopilotClientOptions;
import hudson.model.User;
import java.util.Optional;

/**
 * 🏭 Factory that creates authenticated {@link CopilotClient} instances.
 *
 * <p>A {@link CopilotClient} is the entry point into the Copilot Java SDK. Before the SDK can talk
 * to the AI, it needs the authenticated user's GitHub token and, optionally, a local path to the
 * Copilot CLI binary ({@link CopilotChatConfiguration#getCliPath()}). The SDK will spawn (or reuse)
 * a local CLI process for that user.
 *
 * <p>The factory reads the user's stored token from {@link GitHubTokenStore} and builds the
 * appropriate {@link CopilotClientOptions} based on the current configuration.
 */
public class CopilotClientFactory {
    private final GitHubTokenStore tokenStore;

    public CopilotClientFactory(GitHubTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    /**
     * 🔨 Creates a new {@link CopilotClient} configured for the given Jenkins user.
     *
     * <p>Decision logic:
     *
     * <ol>
     *   <li>Retrieve the user's GitHub token from {@link GitHubTokenStore}. Throws {@link
     *       IllegalStateException} if the user is not authenticated.
        *   <li>Set the GitHub token directly and optionally the local CLI binary path.
     * </ol>
     *
     * @param user the currently logged-in Jenkins user
     * @param configuration the global plugin configuration
     * @return a freshly constructed {@link CopilotClient} (not yet started)
     * @throws IllegalStateException if the user has not authenticated with GitHub
     */
    public CopilotClient createFor(User user, CopilotChatConfiguration configuration) {
        Optional<String> token = tokenStore.getToken(user);
        if (token.isEmpty()) {
            throw new IllegalStateException("User is not authenticated with GitHub.");
        }
        CopilotClientOptions options = new CopilotClientOptions();
        options.setGitHubToken(token.get()).setUseLoggedInUser(false);
        if (configuration.getCliPath() != null && !configuration.getCliPath().isBlank()) {
            // 📂 Use a specific CLI binary instead of letting the SDK find one on PATH
            options.setCliPath(configuration.getCliPath());
        }
        return new CopilotClient(options);
    }
}
