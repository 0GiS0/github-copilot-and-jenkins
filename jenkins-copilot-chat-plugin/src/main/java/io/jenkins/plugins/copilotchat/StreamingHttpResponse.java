package io.jenkins.plugins.copilotchat;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

/**
 * 📶 A Stapler {@link HttpResponse} that streams data using Server-Sent Events (SSE).
 *
 * <p>Server-Sent Events is a W3C standard that allows a server to push a stream of text events to
 * the browser over a single long-lived HTTP connection. The browser-side JavaScript reads these
 * events with {@code new EventSource(...)} or by parsing {@code fetch()} responses.
 *
 * <p>Each event is written to the response in the following format:
 *
 * <pre>{@code
 * data: {"type":"delta","content":"Hello"}
 *
 * data: {"type":"complete"}
 *
 * }</pre>
 *
 * (Note the blank line between events — that is how SSE separates events.)
 *
 * <p>The headers set by {@link #generateResponse} tell the browser and any reverse proxies (e.g.
 * nginx with {@code X-Accel-Buffering: no}) not to buffer the response so that chunks arrive at the
 * client as soon as they are written.
 *
 * <p>The actual writing logic is provided by the {@code streamHandler} lambda, which receives the
 * response {@link PrintWriter} and writes as many events as needed.
 */
public class StreamingHttpResponse implements HttpResponse {
    private final Consumer<PrintWriter> streamHandler;

    /**
     * 🛠️ Creates a new streaming response.
     *
     * @param streamHandler a lambda that receives a {@link PrintWriter} and writes SSE events to it
     */
    public StreamingHttpResponse(Consumer<PrintWriter> streamHandler) {
        this.streamHandler = streamHandler;
    }

    /**
     * 🚀 Sets up the SSE response headers and delegates writing to the stream handler.
     *
     * <p>Important headers:
     *
     * <ul>
     *   <li>{@code Content-Type: text/event-stream} — signals to the browser that this is SSE
     *   <li>{@code Cache-Control: no-cache} — prevents caching of live events
     *   <li>{@code X-Accel-Buffering: no} — disables nginx buffering so chunks are not held back
     * </ul>
     */
    @Override
    public void generateResponse(StaplerRequest2 request, StaplerResponse2 response, Object node)
            throws IOException {
        // 📶 Configure response for Server-Sent Events (SSE)
        response.setContentType("text/event-stream; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        // 🚧 Tell nginx/reverse proxies not to buffer this response
        response.setHeader("X-Accel-Buffering", "no");
        response.setStatus(200);

        PrintWriter writer = response.getWriter();
        try {
            // ✍️ Delegate all event writing to the lambda provided at construction time
            streamHandler.accept(writer);
        } finally {
            writer.close();
        }
    }
}
