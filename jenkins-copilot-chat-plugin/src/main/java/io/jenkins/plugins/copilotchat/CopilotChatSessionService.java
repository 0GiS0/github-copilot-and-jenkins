package io.jenkins.plugins.copilotchat;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.generated.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.generated.AssistantMessageEvent;
import com.github.copilot.sdk.generated.SessionErrorEvent;
import com.github.copilot.sdk.generated.SessionIdleEvent;
import com.github.copilot.sdk.generated.rpc.McpConfigAddParams;
import com.github.copilot.sdk.generated.rpc.SessionMcpEnableParams;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;
import hudson.model.User;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;

public class CopilotChatSessionService {
    private static final Logger LOGGER = Logger.getLogger(CopilotChatSessionService.class.getName());
    private static final String JENKINS_MCP_SERVER_NAME = "jenkins";

    private final CopilotClientFactory clientFactory;
    private final ConcurrentMap<String, UserChatSession> sessions = new ConcurrentHashMap<>();

    public CopilotChatSessionService(CopilotClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public CompletableFuture<String> send(User user, CopilotChatConfiguration configuration, String prompt) {
        return getOrCreateSession(user, configuration).thenCompose(session -> {
            StringBuilder response = new StringBuilder();
            CompletableFuture<String> done = new CompletableFuture<>();
            session.session().on(AssistantMessageDeltaEvent.class, event -> response.append(event.getData().deltaContent()));
            session.session().on(AssistantMessageEvent.class, event -> {
                if (response.isEmpty()) {
                    response.append(event.getData().content());
                }
            });
            session.session().on(SessionErrorEvent.class, event -> done.completeExceptionally(new IllegalStateException(event.getData().message())));
            session.session().on(SessionIdleEvent.class, event -> done.complete(response.toString()));
            session.session().send(new MessageOptions().setPrompt(prompt));
            return done;
        });
    }

    private CompletableFuture<UserChatSession> getOrCreateSession(User user, CopilotChatConfiguration configuration) {
        UserChatSession existing = sessions.get(user.getId());
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                CopilotClient client = clientFactory.createFor(user, configuration);
                client.start().get();
                registerJenkinsMcpServer(client, configuration);
                SessionConfig sessionConfig = new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                        .setModel(configuration.getDefaultModel())
                        .setStreaming(true)
                        .setSystemMessage(new SystemMessageConfig().setContent(buildSystemMessage()));
                List<String> availableTools = parseTools(configuration.getAvailableTools());
                if (!availableTools.isEmpty()) {
                    sessionConfig.setAvailableTools(availableTools);
                }
                CopilotSession session = client.createSession(sessionConfig).get();
                enableJenkinsMcpForSession(session);
                UserChatSession created = new UserChatSession(client, session);
                UserChatSession previous = sessions.putIfAbsent(user.getId(), created);
                return previous == null ? created : previous;
            } catch (Exception e) {
                throw new IllegalStateException("Could not start Copilot chat session", e);
            }
        });
    }

    private void registerJenkinsMcpServer(CopilotClient client, CopilotChatConfiguration configuration) {
        try {
            Map<String, Object> serverConfig = new LinkedHashMap<>();
            serverConfig.put("type", "http");
            serverConfig.put("url", buildJenkinsMcpUrl(configuration));
            String auth = buildJenkinsAuthHeader(configuration);
            if (auth != null) {
                Map<String, String> headers = new LinkedHashMap<>();
                headers.put("Authorization", auth);
                serverConfig.put("headers", headers);
            }
            client.getRpc().mcp.config.add(new McpConfigAddParams(JENKINS_MCP_SERVER_NAME, serverConfig)).get();
            LOGGER.log(Level.FINE, "Registered Jenkins MCP server with Copilot client");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not register Jenkins MCP server: " + e.getMessage(), e);
        }
    }

    private void enableJenkinsMcpForSession(CopilotSession session) {
        try {
            session.getRpc().mcp.enable(new SessionMcpEnableParams(session.getSessionId(), JENKINS_MCP_SERVER_NAME)).get();
            LOGGER.log(Level.FINE, "Enabled Jenkins MCP server for Copilot session");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not enable Jenkins MCP server in session: " + e.getMessage(), e);
        }
    }

    private static String buildJenkinsMcpUrl(CopilotChatConfiguration configuration) {
        String configured = configuration.getJenkinsMcpUrl();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return "http://localhost:8080/mcp-server/stateless";
    }

    private static String buildJenkinsAuthHeader(CopilotChatConfiguration configuration) {
        String username = configuration.getJenkinsMcpUsername();
        String token = configuration.getJenkinsMcpToken();
        if (username == null || username.isBlank() || token == null || token.isBlank()) {
            return null;
        }
        String raw = username + ":" + token;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String buildSystemMessage() {
        String url = Jenkins.get().getRootUrl();
        return "You are GitHub Copilot embedded inside a Jenkins instance"
                + (url != null ? " at " + url : "")
                + ". You have access to a Jenkins MCP server (registered as 'jenkins') that exposes tools to query and operate this Jenkins instance: "
                + "getJobs, getJob, getBuild, getBuildLog, searchBuildLog, getTestResults, triggerBuild, getQueueItem, "
                + "getJobScm, getBuildScm, getBuildChangeSets, findJobsWithScmUrl, whoAmI, getStatus, updateBuild, "
                + "rebuildBuild, replayBuild, getReplayScripts. "
                + "Always prefer calling these tools to answer the user's questions about Jenkins (jobs, builds, logs, status). "
                + "Be concise and use Markdown to format responses. "
                + "Job names inside folders use the form 'folder/job' (for example 'copilot-demos/code-analysis').";
    }

    private static List<String> parseTools(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toList());
    }

    private record UserChatSession(CopilotClient client, CopilotSession session) {}
}