package io.jenkins.plugins.copilotchat;

import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import hudson.util.Secret;

/**
 * 🔑 A Jenkins user property that stores a user's GitHub OAuth access token.
 *
 * <p>Jenkins uses the {@link UserProperty} extension point to attach custom data to each user
 * account. This property stores three pieces of information per Jenkins user:
 *
 * <ul>
 *   <li>{@code token} — the GitHub access token, wrapped in {@link Secret} so Jenkins
 *       encrypts it on disk (stored in {@code $JENKINS_HOME/users/<id>/config.xml}).</li>
 *   <li>{@code githubLogin} — the GitHub username (e.g. {@code octocat}).</li>
 *   <li>{@code githubUserId} — the numeric GitHub user ID.</li>
 * </ul>
 *
 * <p>The class is intentionally simple: no form binding, no UI descriptor — it is managed
 * entirely via {@link GitHubTokenStore}.
 *
 * <p>⚠️ {@link #getDescriptor()} returns {@code null} on purpose because this property should
 * not appear in the user configuration form.
 */
public class CopilotTokenUserProperty extends UserProperty {
    private final Secret token;
    private final String githubLogin;
    private final long githubUserId;

    public CopilotTokenUserProperty(Secret token, String githubLogin, long githubUserId) {
        this.token = token;
        this.githubLogin = githubLogin;
        this.githubUserId = githubUserId;
    }

    /** 🔐 Returns the encrypted GitHub access token. */
    public Secret getToken() {
        return token;
    }

    /** 👤 Returns the GitHub username associated with this token. */
    public String getGithubLogin() {
        return githubLogin;
    }

    /** 🆔 Returns the numeric GitHub user ID. */
    public long getGithubUserId() {
        return githubUserId;
    }

    /**
     * ⚠️ Returns {@code null} intentionally.
     * This property has no UI form, so it does not need a descriptor.
     */
    @Override
    public UserPropertyDescriptor getDescriptor() {
        return null;
    }
}
