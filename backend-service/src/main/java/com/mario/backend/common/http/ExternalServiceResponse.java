package com.mario.backend.common.http;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

@Getter
public class ExternalServiceResponse {

    private final String code;
    private final String status;
    private final String message;
    private final JsonNode data;

    public ExternalServiceResponse(JsonNode jsonNode) {
        this.code = extractText(jsonNode, "code");
        this.status = extractText(jsonNode, "status");
        this.message = extractText(jsonNode, "message", "Unknown response");
        this.data = jsonNode.has("data") ? jsonNode.get("data") : null;
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
