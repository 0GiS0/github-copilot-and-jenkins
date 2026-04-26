package io.jenkins.plugins.copilotchat;

import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.generated.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.generated.AssistantMessageEvent;
import com.github.copilot.sdk.generated.SessionErrorEvent;
import com.github.copilot.sdk.generated.SessionIdleEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PermissionRequestResultKind;
import com.github.copilot.sdk.json.SessionConfig;
import hudson.model.User;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class CopilotChatSessionService {
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
                PermissionHandler denyAll = (request, invocation) -> CompletableFuture.completedFuture(
                    new PermissionRequestResult().setKind(PermissionRequestResultKind.DENIED_BY_RULES));
                SessionConfig sessionConfig = new SessionConfig()
                    .setOnPermissionRequest(denyAll)
                        .setModel(configuration.getDefaultModel())
                        .setStreaming(true);
                List<String> availableTools = parseTools(configuration.getAvailableTools());
                if (!availableTools.isEmpty()) {
                    sessionConfig.setAvailableTools(availableTools);
                }
                CopilotSession session = client.createSession(sessionConfig).get();
                UserChatSession created = new UserChatSession(client, session);
                UserChatSession previous = sessions.putIfAbsent(user.getId(), created);
                return previous == null ? created : previous;
            } catch (Exception e) {
                throw new IllegalStateException("Could not start Copilot chat session", e);
            }
        });
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