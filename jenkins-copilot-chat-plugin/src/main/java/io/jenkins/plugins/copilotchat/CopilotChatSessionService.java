package io.jenkins.plugins.copilotchat;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.generated.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.generated.AssistantMessageEvent;
import com.github.copilot.sdk.generated.AssistantReasoningEvent;
import com.github.copilot.sdk.generated.SessionErrorEvent;
import com.github.copilot.sdk.generated.SessionIdleEvent;
import com.github.copilot.sdk.generated.SessionMcpServersLoadedEvent;
import com.github.copilot.sdk.json.McpHttpServerConfig;
import com.github.copilot.sdk.json.McpServerConfig;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.ModelInfo;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PermissionRequest;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PermissionRequestResultKind;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;
import hudson.model.User;
import java.io.Closeable;
import java.io.IOException;
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
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;

public class CopilotChatSessionService {
    private static final Logger LOGGER = Logger.getLogger(CopilotChatSessionService.class.getName());
    private static final String JENKINS_MCP_SERVER_NAME = "jenkins";
    private static final String GITHUB_MCP_SERVER_NAME = "github";

    private final CopilotClientFactory clientFactory;
    private final ConcurrentMap<String, UserChatSession> sessions = new ConcurrentHashMap<>();

    public CopilotChatSessionService(CopilotClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /**
     * List available models for the user.
     * Uses an existing session's client if available, otherwise creates a temporary client.
     */
    public CompletableFuture<List<ModelInfo>> listModels(User user, CopilotChatConfiguration configuration) {
        UserChatSession existing = sessions.get(user.getId());
        if (existing != null) {
            return existing.client().listModels();
        }
        // No existing session - create a temporary client just for listing models
        return CompletableFuture.supplyAsync(() -> {
            try {
                CopilotClient client = clientFactory.createFor(user, configuration);
                client.start().get(30, TimeUnit.SECONDS);
                List<ModelInfo> models = client.listModels().get(30, TimeUnit.SECONDS);
                client.stop();
                return models;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to list models: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<String> send(User user, CopilotChatConfiguration configuration, String prompt, String pagePath, String model) {
        String effectiveModel = (model != null && !model.isBlank()) ? model : configuration.getDefaultModel();
        return getOrCreateSession(user, configuration, effectiveModel).thenCompose(session -> {
            StringBuilder response = new StringBuilder();
            CompletableFuture<String> done = new CompletableFuture<>();
            List<Closeable> listeners = new java.util.ArrayList<>();
            listeners.add(session.session().on(AssistantMessageDeltaEvent.class, event -> response.append(event.getData().deltaContent())));
            listeners.add(session.session().on(AssistantMessageEvent.class, event -> {
                if (response.isEmpty()) {
                    response.append(event.getData().content());
                }
            }));
            listeners.add(session.session().on(SessionErrorEvent.class, event -> {
                closeQuietly(listeners);
                done.completeExceptionally(new IllegalStateException(event.getData().message()));
            }));
            listeners.add(session.session().on(SessionIdleEvent.class, event -> {
                closeQuietly(listeners);
                done.complete(response.toString());
            }));
            String enrichedPrompt = enrichPromptWithContext(prompt, pagePath);
            session.session().send(new MessageOptions().setPrompt(enrichedPrompt));
            return done;
        });
    }

    public CompletableFuture<Void> sendStream(User user, CopilotChatConfiguration configuration, String prompt, String pagePath, String model, Consumer<String> deltaConsumer, Consumer<String> reasoningConsumer, Consumer<String> completeConsumer, Consumer<Throwable> errorConsumer) {
        String effectiveModel = (model != null && !model.isBlank()) ? model : configuration.getDefaultModel();
        return getOrCreateSession(user, configuration, effectiveModel).thenCompose(session -> {
            CompletableFuture<Void> done = new CompletableFuture<>();
            List<Closeable> listeners = new java.util.ArrayList<>();
            listeners.add(session.session().on(AssistantReasoningEvent.class, event -> {
                String content = event.getData().content();
                if (content != null && !content.isEmpty()) {
                    reasoningConsumer.accept(content);
                }
            }));
            listeners.add(session.session().on(AssistantMessageDeltaEvent.class, event -> {
                String delta = event.getData().deltaContent();
                if (delta != null && !delta.isEmpty()) {
                    deltaConsumer.accept(delta);
                }
            }));
            listeners.add(session.session().on(AssistantMessageEvent.class, event -> {
                String content = event.getData().content();
                if (content != null && !content.isEmpty()) {
                    completeConsumer.accept(content);
                }
            }));
            listeners.add(session.session().on(SessionErrorEvent.class, event -> {
                closeQuietly(listeners);
                errorConsumer.accept(new IllegalStateException(event.getData().message()));
                done.completeExceptionally(new IllegalStateException(event.getData().message()));
            }));
            listeners.add(session.session().on(SessionIdleEvent.class, event -> {
                closeQuietly(listeners);
                done.complete(null);
            }));
            String enrichedPrompt = enrichPromptWithContext(prompt, pagePath);
            session.session().send(new MessageOptions().setPrompt(enrichedPrompt));
            return done;
        });
    }

    private static void closeQuietly(List<Closeable> closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) c.close();
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Failed to remove event listener", e);
            }
        }
    }

    private static String enrichPromptWithContext(String prompt, String pagePath) {
        if (pagePath == null || pagePath.isBlank()) {
            return prompt;
        }
        return "[Jenkins page context: " + pagePath + "]\n\n" + prompt;
    }

    private CompletableFuture<UserChatSession> getOrCreateSession(User user, CopilotChatConfiguration configuration, String model) {
        UserChatSession existing = sessions.get(user.getId());
        // If session exists but uses a different model, invalidate it
        if (existing != null && !model.equals(existing.model())) {
            LOGGER.log(Level.INFO, "Model changed from {0} to {1}, recreating session", new Object[]{existing.model(), model});
            invalidateSession(user);
            existing = null;
        }
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        final String effectiveModel = model;
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
                        .setModel(effectiveModel)
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
                UserChatSession created = new UserChatSession(client, session, effectiveModel);
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

        // GitHub MCP server for repository operations (create PRs, edit files, etc.)
        String githubMcpUrl = configuration.getGithubMcpUrl();
        String githubMcpToken = configuration.getGithubMcpToken();
        if (githubMcpUrl != null && !githubMcpUrl.isBlank()
                && githubMcpToken != null && !githubMcpToken.isBlank()) {
            McpHttpServerConfig github = new McpHttpServerConfig().setUrl(githubMcpUrl);
            github.setTools(List.of("*"));
            Map<String, String> ghHeaders = new LinkedHashMap<>();
            ghHeaders.put("Authorization", "Bearer " + githubMcpToken);
            github.setHeaders(ghHeaders);
            servers.put(GITHUB_MCP_SERVER_NAME, github);
        }

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
                + "You also have access to a GitHub MCP server (registered as 'github') that exposes tools to interact with GitHub repositories: "
                + "create branches, read and edit files, create pull requests, search issues, etc. "
                + "When the user asks you to modify a file (like a Jenkinsfile), use the Jenkins MCP tools to find the SCM repository URL "
                + "and then use the GitHub MCP tools to create a branch, make the changes, and open a pull request. "
                + "The user message includes a [Jenkins page context: ...] prefix with the current page path so you know which job they are viewing. "
                + "Use getJobScm with the job name derived from the path to find the repository. "
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

    /**
     * Invalidate the session for a user, forcing a new session to be created on the next request.
     */
    public void invalidateSession(User user) {
        UserChatSession removed = sessions.remove(user.getId());
        if (removed != null) {
            try {
                removed.client().stop();
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Error stopping client during session invalidation", e);
            }
        }
    }

    private record UserChatSession(CopilotClient client, CopilotSession session, String model) {}
}