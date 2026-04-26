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
    private String availableTools = "read_file,search_code,list_dir";
    private int requestTimeoutSeconds = 120;

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