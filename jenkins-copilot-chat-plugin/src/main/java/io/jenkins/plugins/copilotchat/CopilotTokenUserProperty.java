package io.jenkins.plugins.copilotchat;

import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import hudson.util.Secret;

public class CopilotTokenUserProperty extends UserProperty {
    private final Secret token;
    private final String githubLogin;
    private final long githubUserId;

    public CopilotTokenUserProperty(Secret token, String githubLogin, long githubUserId) {
        this.token = token;
        this.githubLogin = githubLogin;
        this.githubUserId = githubUserId;
    }

    public Secret getToken() {
        return token;
    }

    public String getGithubLogin() {
        return githubLogin;
    }

    public long getGithubUserId() {
        return githubUserId;
    }

    @Override
    public UserPropertyDescriptor getDescriptor() {
        return null;
    }
}