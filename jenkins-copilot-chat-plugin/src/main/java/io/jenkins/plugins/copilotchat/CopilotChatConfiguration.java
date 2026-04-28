package io.jenkins.plugins.copilotchat;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;

@Extension
public class CopilotChatConfiguration extends GlobalConfiguration {
    private static final String DEFAULT_MODEL = "gpt-4.1";

    private String clientId;
    private String cliPath;
    private String defaultModel = DEFAULT_MODEL;
    private String availableTools = "";
    private int requestTimeoutSeconds = 120;
    private String jenkinsMcpUrl;
    private String jenkinsMcpUsername = "admin";
    private String jenkinsMcpToken = "admin";
    private String githubMcpUrl;
    private String githubMcpToken;

    public CopilotChatConfiguration() {
        load();
    }

    public static CopilotChatConfiguration get() {
        return GlobalConfiguration.all().get(CopilotChatConfiguration.class);
    }

    @CheckForNull
    public String getClientId() {
        return clientId;
    }

    @DataBoundSetter
    public void setClientId(String clientId) {
        this.clientId = normalize(clientId);
        save();
    }

    @CheckForNull
    public String getCliPath() {
        return cliPath;
    }

    @DataBoundSetter
    public void setCliPath(String cliPath) {
        this.cliPath = normalize(cliPath);
        save();
    }

    public String getDefaultModel() {
        return defaultModel == null || defaultModel.isBlank() ? DEFAULT_MODEL : defaultModel;
    }

    @DataBoundSetter
    public void setDefaultModel(String defaultModel) {
        this.defaultModel = normalize(defaultModel);
        save();
    }

    public String getAvailableTools() {
        return availableTools == null ? "" : availableTools;
    }

    @DataBoundSetter
    public void setAvailableTools(String availableTools) {
        this.availableTools = normalize(availableTools);
        save();
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds <= 0 ? 120 : requestTimeoutSeconds;
    }

    @DataBoundSetter
    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        save();
    }

    @CheckForNull
    public String getJenkinsMcpUrl() {
        return jenkinsMcpUrl;
    }

    @DataBoundSetter
    public void setJenkinsMcpUrl(String jenkinsMcpUrl) {
        this.jenkinsMcpUrl = normalize(jenkinsMcpUrl);
        save();
    }

    public String getJenkinsMcpUsername() {
        return jenkinsMcpUsername == null || jenkinsMcpUsername.isBlank() ? "admin" : jenkinsMcpUsername;
    }

    @DataBoundSetter
    public void setJenkinsMcpUsername(String jenkinsMcpUsername) {
        this.jenkinsMcpUsername = normalize(jenkinsMcpUsername);
        save();
    }

    @CheckForNull
    public String getJenkinsMcpToken() {
        return jenkinsMcpToken;
    }

    @DataBoundSetter
    public void setJenkinsMcpToken(String jenkinsMcpToken) {
        this.jenkinsMcpToken = normalize(jenkinsMcpToken);
        save();
    }

    @CheckForNull
    public String getGithubMcpUrl() {
        return githubMcpUrl;
    }

    @DataBoundSetter
    public void setGithubMcpUrl(String githubMcpUrl) {
        this.githubMcpUrl = normalize(githubMcpUrl);
        save();
    }

    @CheckForNull
    public String getGithubMcpToken() {
        return githubMcpToken;
    }

    @DataBoundSetter
    public void setGithubMcpToken(String githubMcpToken) {
        this.githubMcpToken = normalize(githubMcpToken);
        save();
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}