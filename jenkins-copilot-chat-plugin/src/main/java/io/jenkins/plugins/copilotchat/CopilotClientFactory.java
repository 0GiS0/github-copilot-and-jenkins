package io.jenkins.plugins.copilotchat;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.CopilotClientOptions;
import hudson.model.User;
import java.util.Optional;

/**
 * 🏭 Factory that creates authenticated {@link CopilotClient} instances.
 *
 * <p>A {@link CopilotClient} is the entry point into the Copilot Java SDK.
 * Before the SDK can talk to the AI, it needs to know either:
 * <ul>
 *   <li>🌐 <b>Remote CLI mode</b> — the URL of a running Copilot CLI HTTP server
 *       ({@link CopilotChatConfiguration#getCliUrl()}). In this mode the CLI server
 *       already holds its own credentials; the plugin doesn't pass any token.</li>
 *   <li>🔑 <b>Local CLI mode</b> — the user's GitHub token and, optionally, a local path to the
 *       Copilot CLI binary ({@link CopilotChatConfiguration#getCliPath()}).
 *       The SDK will spawn (or reuse) a local CLI process.</li>
 * </ul>
 *
 * <p>The factory reads the user's stored token from {@link GitHubTokenStore} and
 * builds the appropriate {@link CopilotClientOptions} based on the current configuration.
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
     * <ol>
     *   <li>Retrieve the user's GitHub token from {@link GitHubTokenStore}.
     *       Throws {@link IllegalStateException} if the user is not authenticated.</li>
     *   <li>If a CLI server URL is configured, set {@code cliUrl} — no token is passed
     *       because the CLI server manages its own authentication.</li>
     *   <li>Otherwise, set the GitHub token directly and optionally the local CLI binary path.</li>
     * </ol>
     *
     * @param user          the currently logged-in Jenkins user
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
        if (configuration.getCliUrl() != null && !configuration.getCliUrl().isBlank()) {
            // 🌐 Remote CLI server mode: point the SDK at the external CLI HTTP server.
            // Token and useLoggedInUser are NOT allowed in this mode.
            options.setCliUrl(configuration.getCliUrl());
        } else {
            // 🔑 Local mode: pass the GitHub token so the SDK can authenticate.
            options.setGitHubToken(token.get()).setUseLoggedInUser(false);
            if (configuration.getCliPath() != null && !configuration.getCliPath().isBlank()) {
                // 📂 Use a specific CLI binary instead of letting the SDK find one on PATH
                options.setCliPath(configuration.getCliPath());
            }
        }
        return new CopilotClient(options);
    }
}
