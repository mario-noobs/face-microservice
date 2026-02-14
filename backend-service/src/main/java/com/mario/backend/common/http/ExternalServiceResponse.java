package com.mario.backend.common.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

@Getter
public class ExternalServiceResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String code;
    private final String status;
    private final String message;
    private final JsonNode data;

    public ExternalServiceResponse(String responseBody) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(responseBody);
            this.code = extractText(jsonNode, "code");
            this.status = extractText(jsonNode, "status");
            this.message = extractText(jsonNode, "message", "Unknown response");
            this.data = jsonNode.has("data") ? jsonNode.get("data") : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse external service response", e);
        }
    }

    public boolean isSuccess() {
        return "0000".equals(code) || "success".equals(status);
    }

    private static String extractText(JsonNode node, String field) {
        return extractText(node, field, "");
    }

    private static String extractText(JsonNode node, String field, String defaultValue) {
        return node.has(field) ? node.get(field).asText() : defaultValue;
    }
}
