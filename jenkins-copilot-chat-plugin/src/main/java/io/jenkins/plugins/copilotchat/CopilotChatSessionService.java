package io.jenkins.plugins.copilotchat;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.generated.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.generated.AssistantMessageEvent;
import com.github.copilot.sdk.generated.SessionErrorEvent;
import com.github.copilot.sdk.generated.SessionIdleEvent;
import com.github.copilot.sdk.generated.SessionMcpServersLoadedEvent;
import com.github.copilot.sdk.json.McpHttpServerConfig;
import com.github.copilot.sdk.json.McpServerConfig;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PermissionRequest;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PermissionRequestResultKind;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
                PermissionHandler approveAll = (request, invocation) ->
                    CompletableFuture.completedFuture(
                        new PermissionRequestResult()
                            .setKind("approve-once")
                            .setRules(List.of()));
                SessionConfig sessionConfig = new SessionConfig()
                    .setOnPermissionRequest(approveAll)
                        .setModel(configuration.getDefaultModel())
                        .setStreaming(true)
                        .setSystemMessage(new SystemMessageConfig().setContent(buildSystemMessage()));
                Map<String, McpServerConfig> mcpServers = buildMcpServers(configuration);
                if (!mcpServers.isEmpty()) {
                    sessionConfig.setMcpServers(mcpServers);
                }
                List<String> availableTools = parseTools(configuration.getAvailableTools());
                if (!availableTools.isEmpty()) {
                    sessionConfig.setAvailableTools(availableTools);
                }
                CopilotSession session = client.createSession(sessionConfig).get();
                // Wait for MCP servers to load before allowing sends
                if (!mcpServers.isEmpty()) {
                    CountDownLatch mcpReady = new CountDownLatch(1);
                    session.on(SessionMcpServersLoadedEvent.class, event -> mcpReady.countDown());
                    if (!mcpReady.await(30, TimeUnit.SECONDS)) {
                        LOGGER.log(Level.WARNING, "Timed out waiting for MCP servers to load");
                    } else {
                        LOGGER.log(Level.INFO, "MCP servers loaded successfully");
                    }
                }
                UserChatSession created = new UserChatSession(client, session);
                UserChatSession previous = sessions.putIfAbsent(user.getId(), created);
                return previous == null ? created : previous;
            } catch (Exception e) {
                throw new IllegalStateException("Could not start Copilot chat session", e);
            }
        });
    }

    private static Map<String, McpServerConfig> buildMcpServers(CopilotChatConfiguration configuration) {
        Map<String, McpServerConfig> servers = new LinkedHashMap<>();
        McpHttpServerConfig jenkins = new McpHttpServerConfig()
                .setUrl(buildJenkinsMcpUrl(configuration));
        jenkins.setTools(List.of("*"));
        String auth = buildJenkinsAuthHeader(configuration);
        if (auth != null) {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Authorization", auth);
            jenkins.setHeaders(headers);
        }
        servers.put(JENKINS_MCP_SERVER_NAME, jenkins);
        return servers;
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