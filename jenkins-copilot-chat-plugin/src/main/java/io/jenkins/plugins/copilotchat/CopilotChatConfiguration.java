package io.jenkins.plugins.copilotchat;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * ⚙️ Global configuration for the Copilot Chat plugin.
 *
 * <p>This class is automatically registered by Jenkins as a global configuration section (via
 * {@code @Extension} + {@code GlobalConfiguration}). Its fields appear in <em>Manage Jenkins →
 * System</em> and are persisted in {@code copilotChatConfiguration.xml}.
 *
 * <p>Key settings:
 *
 * <ul>
 *   <li>🔑 {@code clientId} — GitHub OAuth App client ID used during the Device Flow login.
 *   <li>🌐 {@code cliUrl} — URL of the remote Copilot CLI server (e.g. {@code
 *       http://copilot-cli:3003}). When set, the plugin forwards requests to this server instead of
 *       spawning a local CLI process.
 *   <li>🤖 {@code defaultModel} — The AI model used for new chat sessions (e.g. {@code gpt-5.4}).
 *   <li>🔧 {@code availableTools} — Comma-separated list of MCP tool names the AI is allowed to
 *       invoke. Leave blank to allow all tools.
 *   <li>⏱️ {@code requestTimeoutSeconds} — Maximum seconds to wait for a Copilot response before
 *       timing out.
 *   <li>🏗️ Jenkins MCP settings — URL, username and API token for the Jenkins MCP server.
 *   <li>🐙 GitHub MCP settings — URL and PAT for the GitHub MCP server.
 * </ul>
 */
@Extension
public class CopilotChatConfiguration extends GlobalConfiguration {
    // 🤖 Default AI model used when none is specified in the chat request
    private static final String DEFAULT_MODEL = "gpt-5.4";

    // 🔑 GitHub OAuth App client ID (registered at github.com/settings/developers)
    private String clientId;
    private String cliPath;
    private String cliUrl = "http://copilot-cli:3003";
    private String defaultModel = DEFAULT_MODEL;
    private String availableTools = "";
    private int requestTimeoutSeconds = 120;
    private String jenkinsMcpUrl;
    private String jenkinsMcpUsername = "admin";
    private String jenkinsMcpToken = "admin";
    private String githubMcpUrl;
    private String githubMcpToken;

    /**
     * 🏗️ Constructor called by Jenkins on startup. {@code load()} reads the persisted XML and
     * populates the fields.
     */
    public CopilotChatConfiguration() {
        load();
    }

    /**
     * 🔍 Convenience accessor — retrieves the singleton instance registered in Jenkins. Use this
     * from other classes instead of calling {@code Jenkins.get().getDescriptor(...)}.
     */
    public static CopilotChatConfiguration get() {
        return GlobalConfiguration.all().get(CopilotChatConfiguration.class);
    }

    /** 🔑 Returns the GitHub OAuth App client ID, or {@code null} if not yet configured. */
    @CheckForNull
    public String getClientId() {
        return clientId;
    }

    /** 🔑 Sets the GitHub OAuth App client ID and persists the configuration. */
    @DataBoundSetter
    public void setClientId(String clientId) {
        this.clientId = normalize(clientId);
        save();
    }

    /**
     * 📂 Returns the local filesystem path to the Copilot CLI binary. Only used when {@link
     * #getCliUrl()} is not set (local mode).
     */
    @CheckForNull
    public String getCliPath() {
        return cliPath;
    }

    /** 📂 Sets the local CLI path and persists the configuration. */
    @DataBoundSetter
    public void setCliPath(String cliPath) {
        this.cliPath = normalize(cliPath);
        save();
    }

    /**
     * 🌐 Returns the URL of the remote Copilot CLI server. When this is set the plugin communicates
     * with the CLI over HTTP instead of spawning a local process, which is the recommended setup
     * inside Docker/Kubernetes.
     */
    @CheckForNull
    public String getCliUrl() {
        return cliUrl;
    }

    /** 🌐 Sets the CLI server URL and persists the configuration. */
    @DataBoundSetter
    public void setCliUrl(String cliUrl) {
        this.cliUrl = normalize(cliUrl);
        save();
    }

    /**
     * 🤖 Returns the default AI model ID. Falls back to {@link #DEFAULT_MODEL} when the configured
     * value is blank.
     */
    public String getDefaultModel() {
        return defaultModel == null || defaultModel.isBlank() ? DEFAULT_MODEL : defaultModel;
    }

    /** 🤖 Sets the default AI model and persists the configuration. */
    @DataBoundSetter
    public void setDefaultModel(String defaultModel) {
        this.defaultModel = normalize(defaultModel);
        save();
    }

    /**
     * 🔧 Returns the comma-separated list of MCP tools the AI may use. An empty string means
     * <em>all</em> tools are allowed.
     */
    public String getAvailableTools() {
        return availableTools == null ? "" : availableTools;
    }

    /** 🔧 Sets the available tools list and persists the configuration. */
    @DataBoundSetter
    public void setAvailableTools(String availableTools) {
        this.availableTools = normalize(availableTools);
        save();
    }

    /**
     * ⏱️ Returns the maximum number of seconds to wait for a Copilot response. Defaults to 120 s
     * when the stored value is invalid.
     */
    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds <= 0 ? 120 : requestTimeoutSeconds;
    }

    /** ⏱️ Sets the request timeout and persists the configuration. */
    @DataBoundSetter
    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        save();
    }

    /**
     * 🏗️ Returns the Jenkins MCP server URL. The MCP (Model Context Protocol) server exposes
     * Jenkins operations (get jobs, trigger builds, etc.) as tools the AI can call. Defaults to
     * {@code http://jenkins:8080/mcp-server/stateless} when left blank.
     */
    @CheckForNull
    public String getJenkinsMcpUrl() {
        return jenkinsMcpUrl;
    }

    /** 🏗️ Sets the Jenkins MCP URL and persists the configuration. */
    @DataBoundSetter
    public void setJenkinsMcpUrl(String jenkinsMcpUrl) {
        this.jenkinsMcpUrl = normalize(jenkinsMcpUrl);
        save();
    }

    /** 👤 Returns the Jenkins username used to authenticate against the MCP server. */
    public String getJenkinsMcpUsername() {
        return jenkinsMcpUsername == null || jenkinsMcpUsername.isBlank()
                ? "admin"
                : jenkinsMcpUsername;
    }

    /** 👤 Sets the Jenkins MCP username and persists the configuration. */
    @DataBoundSetter
    public void setJenkinsMcpUsername(String jenkinsMcpUsername) {
        this.jenkinsMcpUsername = normalize(jenkinsMcpUsername);
        save();
    }

    /** 🔐 Returns the Jenkins API token used to authenticate against the MCP server. */
    @CheckForNull
    public String getJenkinsMcpToken() {
        return jenkinsMcpToken;
    }

    /** 🔐 Sets the Jenkins MCP API token and persists the configuration. */
    @DataBoundSetter
    public void setJenkinsMcpToken(String jenkinsMcpToken) {
        this.jenkinsMcpToken = normalize(jenkinsMcpToken);
        save();
    }

    /**
     * 🐙 Returns the GitHub MCP server URL. When configured, the AI can also interact with GitHub
     * repositories (create branches, open PRs, edit files, etc.).
     */
    @CheckForNull
    public String getGithubMcpUrl() {
        return githubMcpUrl;
    }

    /** 🐙 Sets the GitHub MCP server URL and persists the configuration. */
    @DataBoundSetter
    public void setGithubMcpUrl(String githubMcpUrl) {
        this.githubMcpUrl = normalize(githubMcpUrl);
        save();
    }

    /** 🐙 Returns the GitHub Personal Access Token used by the MCP server. */
    @CheckForNull
    public String getGithubMcpToken() {
        return githubMcpToken;
    }

    /** 🐙 Sets the GitHub MCP token and persists the configuration. */
    @DataBoundSetter
    public void setGithubMcpToken(String githubMcpToken) {
        this.githubMcpToken = normalize(githubMcpToken);
        save();
    }

    /**
     * ✅ Returns {@code true} when the minimum required configuration (client ID) is present. Used
     * as a guard before starting the OAuth Device Flow.
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank();
    }

    /**
     * 🧹 Normalizes a string value: trims whitespace and returns {@code null} for blank strings.
     * This keeps stored values clean and allows {@code @CheckForNull} checks to work correctly.
     */
    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
