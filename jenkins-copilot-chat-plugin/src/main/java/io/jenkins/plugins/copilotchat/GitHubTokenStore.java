package io.jenkins.plugins.copilotchat;

import hudson.model.User;
import hudson.util.Secret;
import java.io.IOException;
import java.util.Optional;

/**
 * 🗄️ Persists and retrieves GitHub access tokens for Jenkins users.
 *
 * <p>This class acts as a thin DAO (Data Access Object) on top of Jenkins' user properties system.
 * Each Jenkins {@link User} can have a set of {@link hudson.model.UserProperty} instances attached;
 * this class reads and writes the {@link CopilotTokenUserProperty} on behalf of the calling code.
 *
 * <p>The token itself is stored as a {@link Secret}, meaning Jenkins encrypts it with the master
 * key before writing it to disk. Callers never see the raw token on disk — only the encrypted form.
 *
 * <p>Lifecycle:
 *
 * <ol>
 *   <li>💾 {@link #save} is called by {@link DeviceFlowAuthService} after a successful OAuth login.
 *   <li>🔍 {@link #getToken} / {@link #getIdentity} are called by {@link CopilotClientFactory} and
 *       the auth-status endpoint whenever the token is needed.
 *   <li>🗑️ {@link #delete} is called when the user clicks "Logout".
 * </ol>
 */
public class GitHubTokenStore {
    private static final String TOKEN_PROPERTY_NAME = CopilotTokenUserProperty.class.getName();

    /**
     * 💾 Saves the access token and identity for a Jenkins user. Replaces any previously stored
     * token for the same user.
     *
     * @param user the Jenkins user whose token is being saved
     * @param token the GitHub OAuth access token (will be encrypted by Jenkins)
     * @param login the GitHub username (e.g. {@code octocat})
     * @param githubUserId the numeric GitHub user ID
     */
    public void save(User user, String token, String login, long githubUserId) throws IOException {
        user.addProperty(
                new CopilotTokenUserProperty(Secret.fromString(token), login, githubUserId));
        user.save();
    }

    /**
     * 🔍 Returns the stored GitHub access token for the given user, if present. Returns an empty
     * {@code Optional} when the user has never logged in.
     */
    public Optional<String> getToken(User user) {
        CopilotTokenUserProperty property = getProperty(user);
        if (property == null || property.getToken() == null) {
            return Optional.empty();
        }
        // Secret.toString() decrypts and returns the plain-text token
        return Optional.ofNullable(Secret.toString(property.getToken()));
    }

    /**
     * 👤 Returns the stored GitHub identity (login + numeric ID) for the given user, if present.
     * Used by the {@code /authStatus} endpoint to show who is currently logged in.
     */
    public Optional<GitHubIdentity> getIdentity(User user) {
        CopilotTokenUserProperty property = getProperty(user);
        if (property == null || property.getGithubLogin() == null) {
            return Optional.empty();
        }
        return Optional.of(
                new GitHubIdentity(property.getGithubLogin(), property.getGithubUserId()));
    }

    /**
     * 🗑️ Removes the stored token and identity for the given user (logout). The user will have to
     * go through the Device Flow again to reconnect.
     */
    public void delete(User user) {
        try {
            user.getProperties().remove(TOKEN_PROPERTY_NAME);
            user.save();
        } catch (IOException e) {
            throw new IllegalStateException("Could not remove stored GitHub token", e);
        }
    }

    /** 🔍 Retrieves the raw {@link CopilotTokenUserProperty} from the user, or {@code null}. */
    private CopilotTokenUserProperty getProperty(User user) {
        return user.getProperty(CopilotTokenUserProperty.class);
    }
}
