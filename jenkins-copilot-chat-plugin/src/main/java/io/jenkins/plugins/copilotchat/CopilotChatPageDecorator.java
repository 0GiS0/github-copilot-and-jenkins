package io.jenkins.plugins.copilotchat;

import hudson.Extension;
import hudson.model.PageDecorator;

/**
 * 🎨 Injects the Copilot Chat UI into every Jenkins page.
 *
 * <p>{@link PageDecorator} is a Jenkins extension point that lets plugins add HTML fragments to the
 * page header or footer of <em>all</em> pages rendered by Jenkins.
 *
 * <p>The actual HTML is defined in {@code
 * src/main/resources/.../CopilotChatPageDecorator/footer.jelly}. That Jelly template renders the
 * chat widget markup and loads the CSS/JS assets ({@code copilot-chat.js} and {@code
 * copilot-chat.css}) from the plugin's webapp directory.
 *
 * <p>No Java logic is needed here — the empty class body is intentional. Jenkins uses the presence
 * of the {@code @Extension} annotation to discover and register it.
 */
@Extension
public class CopilotChatPageDecorator extends PageDecorator {}
