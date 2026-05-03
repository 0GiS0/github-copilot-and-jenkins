package io.jenkins.plugins.copilotchat;

/**
 * 👤 Represents a GitHub user's identity after successful authentication.
 *
 * <p>This record is populated by calling {@code GET https://api.github.com/user} with the user's
 * access token. It stores the two key identity fields:
 *
 * <ul>
 *   <li>{@code login} — the GitHub username (e.g. {@code octocat}).
 *   <li>{@code id} — the unique numeric GitHub user ID.
 * </ul>
 *
 * <p>Both values are persisted alongside the access token in {@link CopilotTokenUserProperty} so
 * they can be displayed in the UI without making additional API calls.
 */
public record GitHubIdentity(String login, long id) {}
