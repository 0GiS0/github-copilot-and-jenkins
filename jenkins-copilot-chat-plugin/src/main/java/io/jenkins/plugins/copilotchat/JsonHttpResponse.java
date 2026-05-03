package io.jenkins.plugins.copilotchat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

/**
 * 📦 A Stapler {@link HttpResponse} that serializes a Java object as JSON.
 *
 * <p>Stapler (the MVC framework behind Jenkins) uses {@link HttpResponse} objects returned from
 * action methods to write the HTTP response. This class wraps any Java object, serializes it with
 * Jackson, and writes it with {@code Content-Type: application/json}.
 *
 * <p>Typical usage inside {@link CopilotChatRootAction}:
 *
 * <pre>{@code
 * // Return 200 OK with a JSON body
 * return new JsonHttpResponse(200, Map.of("status", "ok"));
 *
 * // Return 400 Bad Request with an error message
 * return new JsonHttpResponse(400, Map.of("error", "prompt is required"));
 * }</pre>
 */
public class JsonHttpResponse implements HttpResponse {
    // 🔄 Shared ObjectMapper instance — thread-safe and expensive to create, so we reuse it
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final int status;
    private final Object value;

    public JsonHttpResponse(int status, Object value) {
        this.status = status;
        this.value = value;
    }

    /**
     * 🚀 Writes the HTTP response. Sets the status code, content-type header, and serializes {@code
     * value} as JSON directly into the response writer.
     */
    @Override
    public void generateResponse(StaplerRequest2 request, StaplerResponse2 response, Object node)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), value);
    }
}
