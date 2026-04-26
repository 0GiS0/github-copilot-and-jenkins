package io.jenkins.plugins.copilotchat;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.User;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

@Extension
public class CopilotChatRootAction implements RootAction {
    private final GitHubTokenStore tokenStore = new GitHubTokenStore();
    private final DeviceFlowAuthService authService = new DeviceFlowAuthService(tokenStore);
    private final CopilotChatSessionService chatService = new CopilotChatSessionService(new CopilotClientFactory(tokenStore));
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getIconFileName() {
        return "symbol-copilot-chat plugin-copilot-chat";
    }

    @Override
    public String getDisplayName() {
        return "Copilot Chat";
    }

    @Override
    public String getUrlName() {
        return "copilot-chat";
    }

    public HttpResponse doStartLogin() throws IOException, InterruptedException {
        Jenkins.get().checkPermission(Jenkins.READ);
        LoginStart start = authService.startLogin(requireUser(), CopilotChatConfiguration.get());
        return json(start);
    }

    public HttpResponse doPollLogin(@QueryParameter String loginId) throws IOException, InterruptedException {
        Jenkins.get().checkPermission(Jenkins.READ);
        if (loginId == null || loginId.isBlank()) {
            return error("loginId is required");
        }
        return json(authService.pollLogin(requireUser(), loginId, CopilotChatConfiguration.get()));
    }

    public HttpResponse doAuthStatus() {
        Jenkins.get().checkPermission(Jenkins.READ);
        return json(authService.getStoredIdentity(requireUser())
                .<Object>map(identity -> Map.of("authenticated", true, "login", identity.login(), "id", identity.id()))
                .orElseGet(() -> Map.of("authenticated", false)));
    }

    public HttpResponse doLogout() {
        Jenkins.get().checkPermission(Jenkins.READ);
        authService.logout(requireUser());
        return json(Map.of("authenticated", false));
    }

    public void doSendMessage(StaplerRequest2 request, StaplerResponse2 response) throws IOException, ServletException {
        Jenkins.get().checkPermission(Jenkins.READ);
        MessageRequest message = objectMapper.readValue(request.getInputStream(), MessageRequest.class);
        if (message.prompt() == null || message.prompt().isBlank()) {
            error("prompt is required").generateResponse(request, response, null);
            return;
        }
        try {
            String answer = chatService.send(requireUser(), CopilotChatConfiguration.get(), message.prompt()).get();
            json(Map.of("message", answer)).generateResponse(request, response, null);
        } catch (Exception e) {
            error(e.getMessage()).generateResponse(request, response, null);
        }
    }

    private User requireUser() {
        User user = User.current();
        if (user == null) {
            throw new IllegalStateException("A Jenkins user is required.");
        }
        return user;
    }

    private HttpResponse json(Object value) {
        return new JsonHttpResponse(200, value);
    }

    private HttpResponse error(String message) {
        return new JsonHttpResponse(400, Map.of("error", message));
    }
}