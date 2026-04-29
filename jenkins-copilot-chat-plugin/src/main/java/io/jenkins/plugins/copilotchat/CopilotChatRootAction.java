package io.jenkins.plugins.copilotchat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.json.ModelInfo;
import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.User;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

@Extension
public class CopilotChatRootAction implements RootAction {
    private final GitHubTokenStore tokenStore = new GitHubTokenStore();
    private final DeviceFlowAuthService authService = new DeviceFlowAuthService(tokenStore);
    private final CopilotChatSessionService chatService =
            new CopilotChatSessionService(new CopilotClientFactory(tokenStore));
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

    public HttpResponse doStartLogin() {
        Jenkins.get().checkPermission(Jenkins.READ);
        try {
            LoginStart start =
                    authService.startLogin(requireUser(), CopilotChatConfiguration.get());
            return json(start);
        } catch (IllegalStateException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Failed to start login";
            return error(msg);
        }
    }

    public HttpResponse doPollLogin(@QueryParameter String loginId)
            throws IOException, InterruptedException {
        Jenkins.get().checkPermission(Jenkins.READ);
        if (loginId == null || loginId.isBlank()) {
            return error("loginId is required");
        }
        LoginPollResult result =
                authService.pollLogin(requireUser(), loginId, CopilotChatConfiguration.get());
        if ("authenticated".equals(result.status())) {
            chatService.warmUp(requireUser(), CopilotChatConfiguration.get());
        }
        return json(result);
    }

    public HttpResponse doAuthStatus() {
        Jenkins.get().checkPermission(Jenkins.READ);
        boolean isAuthenticated = authService.getStoredIdentity(requireUser()).isPresent();
        if (isAuthenticated) {
            chatService.warmUp(requireUser(), CopilotChatConfiguration.get());
        }
        return json(
                authService
                        .getStoredIdentity(requireUser())
                        .<Object>map(
                                identity ->
                                        Map.of(
                                                "authenticated",
                                                true,
                                                "login",
                                                identity.login(),
                                                "id",
                                                identity.id()))
                        .orElseGet(() -> Map.of("authenticated", false)));
    }

    public HttpResponse doLogout() {
        Jenkins.get().checkPermission(Jenkins.READ);
        authService.logout(requireUser());
        return json(Map.of("authenticated", false));
    }

    public HttpResponse doModels() {
        Jenkins.get().checkPermission(Jenkins.READ);
        try {
            List<ModelInfo> models =
                    chatService
                            .listModels(requireUser(), CopilotChatConfiguration.get())
                            .get(30, TimeUnit.SECONDS);
            String defaultModel = CopilotChatConfiguration.get().getDefaultModel();
            // Convert to simpler format for JSON serialization
            List<Map<String, Object>> modelList =
                    models.stream()
                            .map(
                                    m ->
                                            Map.<String, Object>of(
                                                    "id",
                                                    m.getId(),
                                                    "name",
                                                    m.getName() != null ? m.getName() : m.getId()))
                            .toList();
            return json(
                    Map.of(
                            "models", modelList,
                            "defaultModel", defaultModel));
        } catch (IllegalStateException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Failed to fetch models";
            return error(msg);
        }
    }

    public void doSendMessage(StaplerRequest2 request, StaplerResponse2 response)
            throws IOException, ServletException {
        Jenkins.get().checkPermission(Jenkins.READ);
        MessageRequest message =
                objectMapper.readValue(request.getInputStream(), MessageRequest.class);
        if (message.prompt() == null || message.prompt().isBlank()) {
            error("prompt is required").generateResponse(request, response, null);
            return;
        }

        // Use streaming response
        new StreamingHttpResponse(
                        writer -> {
                            try {
                                chatService
                                        .sendStream(
                                                requireUser(),
                                                CopilotChatConfiguration.get(),
                                                message.prompt(),
                                                message.pagePath(),
                                                message.model(),
                                                // Delta consumer - send each chunk as SSE event
                                                delta -> {
                                                    try {
                                                        writer.write("data: ");
                                                        writer.write(
                                                                objectMapper.writeValueAsString(
                                                                        Map.of(
                                                                                "type", "delta",
                                                                                "content", delta)));
                                                        writer.write("\n\n");
                                                        writer.flush();
                                                    } catch (Exception ex) {
                                                        throw new java.io.UncheckedIOException(
                                                                new java.io.IOException(ex));
                                                    }
                                                },
                                                // Reasoning consumer - send thinking/reasoning as
                                                // SSE event
                                                reasoning -> {
                                                    try {
                                                        writer.write("data: ");
                                                        writer.write(
                                                                objectMapper.writeValueAsString(
                                                                        Map.of(
                                                                                "type",
                                                                                "reasoning",
                                                                                "content",
                                                                                reasoning)));
                                                        writer.write("\n\n");
                                                        writer.flush();
                                                    } catch (Exception ex) {
                                                        throw new java.io.UncheckedIOException(
                                                                new java.io.IOException(ex));
                                                    }
                                                },
                                                // Complete consumer - send final complete event
                                                complete -> {
                                                    try {
                                                        writer.write("data: ");
                                                        writer.write(
                                                                objectMapper.writeValueAsString(
                                                                        Map.of(
                                                                                "type",
                                                                                "complete")));
                                                        writer.write("\n\n");
                                                        writer.flush();
                                                    } catch (Exception ex) {
                                                        throw new java.io.UncheckedIOException(
                                                                new java.io.IOException(ex));
                                                    }
                                                },
                                                // Error consumer
                                                err -> {
                                                    try {
                                                        writer.write("data: ");
                                                        writer.write(
                                                                objectMapper.writeValueAsString(
                                                                        Map.of(
                                                                                "type",
                                                                                "error",
                                                                                "message",
                                                                                err.getMessage())));
                                                        writer.write("\n\n");
                                                        writer.flush();
                                                    } catch (Exception ex) {
                                                        throw new java.io.UncheckedIOException(
                                                                new java.io.IOException(ex));
                                                    }
                                                })
                                        .get(120, java.util.concurrent.TimeUnit.SECONDS);
                            } catch (java.util.concurrent.TimeoutException e) {
                                try {
                                    writer.write("data: ");
                                    writer.write(
                                            objectMapper.writeValueAsString(
                                                    Map.of(
                                                            "type",
                                                            "error",
                                                            "message",
                                                            "Copilot did not respond within 120 seconds")));
                                    writer.write("\n\n");
                                    writer.flush();
                                } catch (Exception ex) {
                                    // Ignore - connection may be closed
                                }
                            } catch (Exception e) {
                                String msg =
                                        e.getCause() != null
                                                ? e.getCause().getMessage()
                                                : e.getMessage();
                                try {
                                    writer.write("data: ");
                                    writer.write(
                                            objectMapper.writeValueAsString(
                                                    Map.of(
                                                            "type",
                                                            "error",
                                                            "message",
                                                            msg == null ? e.toString() : msg)));
                                    writer.write("\n\n");
                                } catch (Exception ex) {
                                    // Ignore - connection may be closed
                                }
                                writer.flush();
                            }
                        })
                .generateResponse(request, response, null);
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
