package io.jenkins.plugins.copilotchat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class JsonHttpResponse implements HttpResponse {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final int status;
    private final Object value;

    public JsonHttpResponse(int status, Object value) {
        this.status = status;
        this.value = value;
    }

    @Override
    public void generateResponse(StaplerRequest2 request, StaplerResponse2 response, Object node)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), value);
    }
}
