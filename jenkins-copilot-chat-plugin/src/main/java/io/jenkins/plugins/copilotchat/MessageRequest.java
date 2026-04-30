package io.jenkins.plugins.copilotchat;

/**
 * 💬 Represents an incoming chat message sent from the browser to the plugin.
 *
 * <p>When the user types a question in the Copilot Chat widget and presses Send,
 * the JavaScript makes a {@code POST} to {@code /copilot-chat/sendMessage} with a JSON body
 * that deserializes into this record:
 *
 * <ul>
 *   <li>{@code prompt} — the user's question or instruction (required).</li>
 *   <li>{@code pagePath} — the current Jenkins page URL path (e.g. {@code /job/my-pipeline/}).
 *       The server prepends this as context so the AI knows which job the user is looking at.</li>
 *   <li>{@code model} — optional model override (e.g. {@code gpt-4o}). Falls back to the
 *       globally configured default when absent.</li>
 * </ul>
 */
public record MessageRequest(String prompt, String pagePath, String model) {}
