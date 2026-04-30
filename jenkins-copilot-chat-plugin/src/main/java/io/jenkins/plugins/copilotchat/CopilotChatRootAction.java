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

/**
 * 🌐 The main REST controller and navigation entry point for the Copilot Chat plugin.
 *
 * <h2>Role in Jenkins</h2>
 * <p>A Jenkins {@link RootAction} is an extension that:
 * <ul>
 *   <li>Adds a top-level URL under Jenkins root (here: {@code /copilot-chat/}).</li>
 *   <li>Optionally adds an icon and link to the Jenkins side panel (if
 *       {@link #getIconFileName()} returns a non-null value).</li>
 *   <li>Acts as a Stapler MVC controller: public {@code doXxx()} methods are
 *       automatically mapped to {@code /copilot-chat/xxx} HTTP endpoints.</li>
 * </ul>
 *
 * <h2>Endpoints exposed</h2>
 * <ul>
 *   <li>📲 {@code GET  /copilot-chat/startLogin}  — starts the GitHub Device Flow</li>
 *   <li>🔄 {@code GET  /copilot-chat/pollLogin}   — polls for Device Flow completion</li>
 *   <li>ℹ️ {@code GET  /copilot-chat/authStatus}  — returns current auth state</li>
 *   <li>🚪 {@code GET  /copilot-chat/logout}      — removes stored token</li>
 *   <li>🤖 {@code GET  /copilot-chat/models}      — lists available AI models</li>
 *   <li>💬 {@code POST /copilot-chat/sendMessage} — streams a chat response via SSE</li>
 * </ul>
 *
 * <p>All endpoints require {@link Jenkins#READ} permission — any authenticated Jenkins user
 * can use the chat, but anonymous access is blocked.
 */
@Extension
public class CopilotChatRootAction implements RootAction {
    // 🗄️ Persistent services wired together at construction time
    private final GitHubTokenStore tokenStore = new GitHubTokenStore();
    private final DeviceFlowAuthService authService = new DeviceFlowAuthService(tokenStore);
    private final CopilotChatSessionService chatService =
            new CopilotChatSessionService(new CopilotClientFactory(tokenStore));
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 🎨 The icon shown in the Jenkins sidebar.
     * Uses Jenkins' symbol icon system — the value refers to a symbol named {@code copilot-chat}
     * registered by this plugin's own resources (see {@code images/symbols/}).
     */
    @Override
    public String getIconFileName() {
        return "symbol-copilot-chat plugin-copilot-chat";
    }

    /** 📛 The display label shown next to the icon in the sidebar. */
    @Override
    public String getDisplayName() {
        return "Copilot Chat";
    }

    /**
     * 🌐 The URL segment under Jenkins root where this action is mounted.
     * Full URL will be: {@code http://jenkins:8080/copilot-chat/}.
     */
    @Override
    public String getUrlName() {
        return "copilot-chat";
    }

    /**
     * 📲 Starts the GitHub Device Flow login.
     *
     * <p>Returns a {@link LoginStart} JSON payload containing the user code and verification URL
     * that the browser will display to guide the user through GitHub authorization.
     */
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

    /**
     * 🔄 Polls the Device Flow to check if the user has authorized the GitHub OAuth App.
     *
     * @param loginId the server-side key returned by {@link #doStartLogin()}
     */
    public HttpResponse doPollLogin(@QueryParameter String loginId)
            throws IOException, InterruptedException {
        Jenkins.get().checkPermission(Jenkins.READ);
        if (loginId == null || loginId.isBlank()) {
            return error("loginId is required");
        }
        return json(authService.pollLogin(requireUser(), loginId, CopilotChatConfiguration.get()));
    }

    /**
     * ℹ️ Returns the current authentication status for the logged-in Jenkins user.
     *
     * <p>The browser calls this on page load to decide whether to show the login button
     * or the chat interface.
     */
    public HttpResponse doAuthStatus() {
        Jenkins.get().checkPermission(Jenkins.READ);
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

    /** 🚪 Removes the user's stored GitHub token, effectively logging them out of Copilot Chat. */
    public HttpResponse doLogout() {
        Jenkins.get().checkPermission(Jenkins.READ);
        authService.logout(requireUser());
        return json(Map.of("authenticated", false));
    }

    /**
     * 🤖 Returns the list of AI models available to the current user.
     *
     * <p>Models are fetched from the Copilot API via the SDK. The list depends on
     * the user's GitHub Copilot subscription level.
     * The response also includes the currently configured default model so the UI
     * can pre-select it.
     */
    public HttpResponse doModels() {
        Jenkins.get().checkPermission(Jenkins.READ);
        try {
            List<ModelInfo> models =
                    chatService
                            .listModels(requireUser(), CopilotChatConfiguration.get())
                            .get(30, TimeUnit.SECONDS);
            String defaultModel = CopilotChatConfiguration.get().getDefaultModel();
            // 🔄 Convert SDK ModelInfo objects to plain maps for JSON serialization
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

    /**\n     * \ud83d\udcac Handles an incoming chat message and streams the AI response via Server-Sent Events.\n     *\n     * <p>Flow:\n     * <ol>\n     *   <li>Deserialize the {@link MessageRequest} JSON body from the HTTP request.</li>\n     *   <li>Validate that the prompt is not blank.</li>\n     *   <li>Open a {@link StreamingHttpResponse} that writes SSE events.</li>\n     *   <li>Pass four callbacks to {@link CopilotChatSessionService#sendStream}:\n     *       <ul>\n     *         <li>\ud83d\udcdd {@code delta} \u2014 partial text chunks as they arrive (streamed token-by-token)</li>\n     *         <li>\ud83e\udde0 {@code reasoning} \u2014 optional thinking steps from reasoning models</li>\n     *         <li>\u2705 {@code complete} \u2014 signals the end of the response</li>\n     *         <li>\u274c {@code error} \u2014 sends an error event if something goes wrong</li>\n     *       </ul>\n     *   </li>\n     * </ol>\n     */\n    public void doSendMessage(StaplerRequest2 request, StaplerResponse2 response)\n            throws IOException, ServletException {\n        Jenkins.get().checkPermission(Jenkins.READ);\n        MessageRequest message =\n                objectMapper.readValue(request.getInputStream(), MessageRequest.class);\n        if (message.prompt() == null || message.prompt().isBlank()) {\n            error(\"prompt is required\").generateResponse(request, response, null);\n            return;\n        }\n\n        // \ud83d\udcf6 Use a streaming response to push SSE events back to the browser\n        new StreamingHttpResponse(
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

    /**
     * \ud83d\udc64 Returns the currently logged-in Jenkins user.
     * Throws an {@link IllegalStateException} if called outside a request context
     * (e.g. background threads), acting as a safety guard.
     */
    private User requireUser() {
        User user = User.current();
        if (user == null) {
            throw new IllegalStateException("A Jenkins user is required.");
        }
        return user;
    }

    /** \ud83d\udce6 Shortcut: wraps a value in a 200 OK JSON response. */
    private HttpResponse json(Object value) {
        return new JsonHttpResponse(200, value);
    }

    /** \u274c Shortcut: wraps an error message in a 400 Bad Request JSON response. */
    private HttpResponse error(String message) {
        return new JsonHttpResponse(400, Map.of("error", message));
    }
}
