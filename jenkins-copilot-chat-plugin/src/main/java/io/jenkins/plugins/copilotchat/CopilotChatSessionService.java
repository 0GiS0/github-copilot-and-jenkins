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
import com.github.copilot.sdk.json.PermissionRequestResult;
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

/**
 * \ud83e\udd16 Manages per-user Copilot chat sessions and handles AI message streaming.
 *
 * <h2>Session lifecycle</h2>
 * <p>A {@link UserChatSession} is lazily created the first time a user sends a message.
 * It bundles:
 * <ul>
 *   <li>A {@link com.github.copilot.sdk.CopilotClient} \u2014 a long-lived connection to the Copilot CLI.</li>
 *   <li>A {@link com.github.copilot.sdk.CopilotSession} \u2014 a stateful conversation with the AI
 *       (keeps context across turns).</li>
 *   <li>The model name for which the session was created.</li>
 * </ul>
 *
 * <p>Sessions are cached in {@link #sessions} (keyed by Jenkins user ID) and reused across
 * requests. If the user switches AI model, the old session is torn down and a new one is started.
 *
 * <h2>MCP integration</h2>
 * <p>When the session is created, it registers one or two MCP (Model Context Protocol) servers:
 * <ul>
 *   <li>\ud83c\udfd7\ufe0f <b>Jenkins MCP</b> \u2014 always registered; exposes Jenkins operations as AI-callable tools.</li>
 *   <li>\ud83d\udc19 <b>GitHub MCP</b> \u2014 registered only when both URL and token are configured; exposes
 *       GitHub repository operations (create PR, edit files, etc.).</li>
 * </ul>
 *
 * <h2>Streaming</h2>
 * <p>The AI response arrives as a stream of SDK events. {@link #sendStream} registers four
 * event listeners before sending the prompt:
 * <ol>
 *   <li>{@code AssistantReasoningEvent} \u2014 thinking steps (reasoning models only)</li>
 *   <li>{@code AssistantMessageDeltaEvent} \u2014 partial text chunks (token-by-token)</li>
 *   <li>{@code AssistantMessageEvent} \u2014 full message (fallback if no deltas were emitted)</li>
 *   <li>{@code SessionIdleEvent} \u2014 signals that the AI has finished responding</li>
 *   <li>{@code SessionErrorEvent} \u2014 signals an error during the turn</li>
 * </ol>
 */
public class CopilotChatSessionService {
    private static final Logger LOGGER =
            Logger.getLogger(CopilotChatSessionService.class.getName());
    // \ud83c\udf10 Names used to register the MCP servers in the session configuration
    private static final String JENKINS_MCP_SERVER_NAME = "jenkins";
    private static final String GITHUB_MCP_SERVER_NAME = "github";

    private final CopilotClientFactory clientFactory;
    // \ud83d\uddc4\ufe0f Thread-safe map from Jenkins user ID to active chat session
    private final ConcurrentMap<String, UserChatSession> sessions = new ConcurrentHashMap<>();

    public CopilotChatSessionService(CopilotClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /**
     * \ud83e\udd16 Lists AI models available to the user.
     *
     * <p>Reuses the client from an existing session when one is available.
     * Otherwise creates a temporary client, fetches the model list, and stops the client.
     */
    public CompletableFuture<List<ModelInfo>> listModels(
            User user, CopilotChatConfiguration configuration) {
        UserChatSession existing = sessions.get(user.getId());
        if (existing != null) {
            // \u267b\ufe0f Reuse the SDK client that's already running \u2014 avoids starting an extra process
            return existing.client().listModels();
        }
        // \ud83d'ude80 No existing session — create a temporary client just for listing models, then stop it
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        CopilotClient client = clientFactory.createFor(user, configuration);
                        client.start().get(30, TimeUnit.SECONDS);
                        List<ModelInfo> models = client.listModels().get(30, TimeUnit.SECONDS);
                        client.stop();
                        return models;
                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "Failed to list models: " + e.getMessage(), e);
                    }
                });
    }

    /**
     * \ud83d\udcac Sends a prompt and collects the full response as a single string (non-streaming).
     *
     * <p>Registers listeners for delta events (partial chunks), the final message, errors,
     * and the idle event that marks the end of the AI turn. Waits for completion asynchronously.
     */
    public CompletableFuture<String> send(
            User user,
            CopilotChatConfiguration configuration,
            String prompt,
            String pagePath,
            String model) {
        String effectiveModel =
                (model != null && !model.isBlank()) ? model : configuration.getDefaultModel();
        return getOrCreateSession(user, configuration, effectiveModel)
                .thenCompose(
                        session -> {
                            StringBuilder response = new StringBuilder();
                            CompletableFuture<String> done = new CompletableFuture<>();
                            List<Closeable> listeners = new java.util.ArrayList<>();
                            listeners.add(
                                    session.session()
                                            .on(
                                                    AssistantMessageDeltaEvent.class,
                                                    event ->
                                                            response.append(
                                                                    event.getData()
                                                                            .deltaContent())));
                            listeners.add(
                                    session.session()
                                            .on(
                                                    AssistantMessageEvent.class,
                                                    event -> {
                                                        if (response.isEmpty()) {
                                                            response.append(
                                                                    event.getData().content());
                                                        }
                                                    }));
                            listeners.add(
                                    session.session()
                                            .on(
                                                    SessionErrorEvent.class,
                                                    event -> {
                                                        closeQuietly(listeners);
                                                        done.completeExceptionally(
                                                                new IllegalStateException(
                                                                        event.getData().message()));
                                                    }));
                            listeners.add(
                                    session.session()
                                            .on(
                                                    SessionIdleEvent.class,
                                                    event -> {
                                                        closeQuietly(listeners);
                                                        done.complete(response.toString());
                                                    }));
                            String enrichedPrompt = enrichPromptWithContext(prompt, pagePath);
                            session.session().send(new MessageOptions().setPrompt(enrichedPrompt));
                            return done;
                        });
    }

    /**
     * \ud83d\udcf6 Sends a prompt and streams the AI response token-by-token via callback consumers.
     *
     * <p>This is the primary method called by {@link CopilotChatRootAction#doSendMessage}.
     * It uses four callbacks so the caller can react to different event types independently:
     *
     * @param deltaConsumer     called for each incremental text chunk (partial response)
     * @param reasoningConsumer called with reasoning/thinking steps (reasoning models only)
     * @param completeConsumer  called once with the full message when the turn ends
     * @param errorConsumer     called if the session reports an error
     */
    public CompletableFuture<Void> sendStream(
            User user,
            CopilotChatConfiguration configuration,
            String prompt,
            String pagePath,
            String model,
            Consumer<String> deltaConsumer,
            Consumer<String> reasoningConsumer,
            Consumer<String> completeConsumer,
            Consumer<Throwable> errorConsumer) {
        String effectiveModel =
                (model != null && !model.isBlank()) ? model : configuration.getDefaultModel();
        return getOrCreateSession(user, configuration, effectiveModel)
                .thenCompose(
                        session -> {
                            CompletableFuture<Void> done = new CompletableFuture<>();
                            List<Closeable> listeners = new java.util.ArrayList<>();
                            // \ud83e\udde0 Reasoning events: thinking steps from reasoning models (e.g. o1, o3)
                            listeners.add(
                                    session.session()
                                            .on(
                                                    AssistantReasoningEvent.class,
                                                    event -> {
                                                        String content = event.getData().content();
                                                        if (content != null && !content.isEmpty()) {
                                                            reasoningConsumer.accept(content);
                                                        }
                                                    }));
                            listeners.add(
                                    session.session()
                                            .on(
                                                    AssistantMessageDeltaEvent.class,
                                                    event -> {
                                                        String delta =
                                                                event.getData().deltaContent();
                                                        if (delta != null && !delta.isEmpty()) {
                                                            deltaConsumer.accept(delta);
                                                        }
                                                    }));
                            listeners.add(
                                    session.session()
                                            .on(
                                                    AssistantMessageEvent.class,
                                                    event -> {
                                                        String content = event.getData().content();
                                                        if (content != null && !content.isEmpty()) {
                                                            completeConsumer.accept(content);
                                                        }
                                                    }));
                            listeners.add(
                                    session.session()
                                            .on(
                                                    SessionErrorEvent.class,
                                                    event -> {
                                                        closeQuietly(listeners);
                                                        errorConsumer.accept(
                                                                new IllegalStateException(
                                                                        event.getData().message()));
                                                        done.completeExceptionally(
                                                                new IllegalStateException(
                                                                        event.getData().message()));
                                                    }));
                            listeners.add(
                                    session.session()
                                            .on(
                                                    SessionIdleEvent.class,
                                                    event -> {
                                                        closeQuietly(listeners);
                                                        done.complete(null);
                                                    }));
                            String enrichedPrompt = enrichPromptWithContext(prompt, pagePath);
                            session.session().send(new MessageOptions().setPrompt(enrichedPrompt));
                            return done;
                        });
    }

    /**
     * 🧹 Closes a list of {@link Closeable} event listener handles quietly.
     * Called after a chat turn completes (or fails) to deregister all listeners and
     * avoid memory leaks from leftover event subscriptions on the session.
     */
    private static void closeQuietly(List<Closeable> closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) c.close();
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Failed to remove event listener", e);
            }
        }
    }

    /**
     * 🗺️ Prepends the current Jenkins page path to the user's prompt as extra context.
     *
     * <p>Knowing which page the user is on helps the AI answer job-specific questions
     * without the user having to mention the job name explicitly.
     * Example result: {@code "[Jenkins page context: /job/my-pipeline/]\n\nWhy did the last build fail?"}
     */
    private static String enrichPromptWithContext(String prompt, String pagePath) {
        if (pagePath == null || pagePath.isBlank()) {
            return prompt;
        }
        return "[Jenkins page context: " + pagePath + "]\n\n" + prompt;
    }

    /**
     * 🔄 Returns the existing session for the user, or creates a new one.
     *
     * <p>If a session already exists for the requested model it is reused — this preserves
     * conversation history across multiple turns in the same chat session.
     *
     * <p>If the user has changed the AI model, the old session is torn down first so that
     * the new session uses the correct model from the start.
     *
     * <p>Session creation steps:
     * <ol>
     *   <li>Create and start a {@link CopilotClient}.</li>
     *   <li>Build the {@link SessionConfig} with model, system message, MCP servers, and tools.</li>
     *   <li>Call {@link com.github.copilot.sdk.CopilotClient#createSession} to start the AI session.</li>
     *   <li>Wait up to 30 seconds for MCP servers to initialize (if any were configured).</li>
     *   <li>Store the session in {@link #sessions} using a compare-and-set to handle races.</li>
     * </ol>
     */
    private CompletableFuture<UserChatSession> getOrCreateSession(
            User user, CopilotChatConfiguration configuration, String model) {
        UserChatSession existing = sessions.get(user.getId());
        // 🔄 If session exists but uses a different model, invalidate it and create a fresh one
        if (existing != null && !model.equals(existing.model())) {
            LOGGER.log(
                    Level.INFO,
                    "Model changed from {0} to {1}, recreating session",
                    new Object[] {existing.model(), model});
            invalidateSession(user);
            existing = null;
        }
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        final String effectiveModel = model;
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        CopilotClient client = clientFactory.createFor(user, configuration);
                        client.start().get();
                        PermissionHandler approveAll =
                                (request, invocation) ->
                                        CompletableFuture.completedFuture(
                                                new PermissionRequestResult()
                                                        .setKind("approve-once")
                                                        .setRules(List.of()));
                        SessionConfig sessionConfig =
                                new SessionConfig()
                                        .setOnPermissionRequest(approveAll)
                                        .setModel(effectiveModel)
                                        .setStreaming(true)
                                        .setSystemMessage(
                                                new SystemMessageConfig()
                                                        .setContent(buildSystemMessage()));
                        Map<String, McpServerConfig> mcpServers = buildMcpServers(configuration);
                        if (!mcpServers.isEmpty()) {
                            sessionConfig.setMcpServers(mcpServers);
                        }
                        List<String> availableTools = parseTools(configuration.getAvailableTools());
                        if (!availableTools.isEmpty()) {
                            sessionConfig.setAvailableTools(availableTools);
                        }
                        CopilotSession session = client.createSession(sessionConfig).get();
                        // \u23f3 Wait for MCP servers to initialize before sending any messages.
                        // Without this, the AI might not yet have access to Jenkins/GitHub tools.
                        if (!mcpServers.isEmpty()) {
                            CountDownLatch mcpReady = new CountDownLatch(1);
                            session.on(
                                    SessionMcpServersLoadedEvent.class,
                                    event -> mcpReady.countDown());
                            if (!mcpReady.await(30, TimeUnit.SECONDS)) {
                                LOGGER.log(
                                        Level.WARNING, "Timed out waiting for MCP servers to load");
                            } else {
                                LOGGER.log(Level.INFO, "MCP servers loaded successfully");
                            }
                        }
                        UserChatSession created =
                                new UserChatSession(client, session, effectiveModel);
                        // \ud83d\udd12 Use putIfAbsent to handle the rare race where two threads create a session
                        // concurrently \u2014 only the first one wins; the other is discarded.
                        UserChatSession previous = sessions.putIfAbsent(user.getId(), created);
                        return previous == null ? created : previous;
                    } catch (Exception e) {
                        throw new IllegalStateException("Could not start Copilot chat session", e);
                    }
                });
    }

    /**
     * \ud83c\udfd7\ufe0f Builds the MCP server configuration map for a new session.
     *
     * <p>The Jenkins MCP server is always registered. The GitHub MCP server is only registered
     * when both its URL and token are present in the configuration.
     */
    private static Map<String, McpServerConfig> buildMcpServers(
            CopilotChatConfiguration configuration) {
        Map<String, McpServerConfig> servers = new LinkedHashMap<>();
        McpHttpServerConfig jenkins =
                new McpHttpServerConfig().setUrl(buildJenkinsMcpUrl(configuration));
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
        if (githubMcpUrl != null
                && !githubMcpUrl.isBlank()
                && githubMcpToken != null
                && !githubMcpToken.isBlank()) {
            McpHttpServerConfig github = new McpHttpServerConfig().setUrl(githubMcpUrl);
            github.setTools(List.of("*"));
            Map<String, String> ghHeaders = new LinkedHashMap<>();
            ghHeaders.put("Authorization", "Bearer " + githubMcpToken);
            github.setHeaders(ghHeaders);
            servers.put(GITHUB_MCP_SERVER_NAME, github);
        }

        return servers;
    }

    /**
     * 🏗️ Returns the Jenkins MCP server URL.
     * Falls back to {@code http://jenkins:8080/mcp-server/stateless} (the default Docker Compose
     * address) when no URL is explicitly configured.
     */
    private static String buildJenkinsMcpUrl(CopilotChatConfiguration configuration) {
        String configured = configuration.getJenkinsMcpUrl();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return "http://jenkins:8080/mcp-server/stateless";
    }

    /**
     * 🔐 Builds the HTTP {@code Authorization: Basic ...} header for the Jenkins MCP server.
     * Returns {@code null} when credentials are not configured (unauthenticated access).
     */
    private static String buildJenkinsAuthHeader(CopilotChatConfiguration configuration) {
        String username = configuration.getJenkinsMcpUsername();
        String token = configuration.getJenkinsMcpToken();
        if (username == null || username.isBlank() || token == null || token.isBlank()) {
            return null;
        }
        String raw = username + ":" + token;
        return "Basic "
                + Base64.getEncoder()
                        .encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 🤖 Builds the system message that defines the AI's persona and capabilities.
     *
     * <p>The system message is injected at session creation time and stays in the AI's context
     * for all turns in the conversation. It tells the AI:
     * <ul>
     *   <li>Who it is (GitHub Copilot embedded in Jenkins)</li>
     *   <li>Which MCP tools are available and what they do</li>
     *   <li>How to interpret the page context prefix added by {@link #enrichPromptWithContext}</li>
     * </ul>
     */
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

    /**
     * 🔧 Parses a comma-separated tool list from the configuration string.
     * Returns an empty list (meaning "allow all") when the configuration is blank.
     */
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
     * 🗑️ Invalidates and stops the session for a user.
     * Forces a brand-new session to be created on the user's next message.
     * Called automatically when the model changes, and can also be invoked externally.
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

    /**
     * 🗄️ Immutable value object that groups the three components of an active chat session.
     *
     * <ul>
     *   <li>{@code client} — the SDK client connected to the Copilot CLI process/server</li>
     *   <li>{@code session} — the stateful AI conversation (preserves history across turns)</li>
     *   <li>{@code model} — the model name used when creating this session (used for change detection)</li>
     * </ul>
     */
    private record UserChatSession(CopilotClient client, CopilotSession session, String model) {}
}
