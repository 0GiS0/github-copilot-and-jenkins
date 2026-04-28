package io.jenkins.plugins.copilotchat;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class StreamingHttpResponse implements HttpResponse {
    private final Consumer<PrintWriter> streamHandler;

    public StreamingHttpResponse(Consumer<PrintWriter> streamHandler) {
        this.streamHandler = streamHandler;
    }

    @Override
    public void generateResponse(StaplerRequest2 request, StaplerResponse2 response, Object node) throws IOException {
        response.setContentType("text/event-stream");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        response.setStatus(200);

        PrintWriter writer = response.getWriter();
        try {
            streamHandler.accept(writer);
        } finally {
            writer.close();
        }
    }
}
