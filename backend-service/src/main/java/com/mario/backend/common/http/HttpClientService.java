package com.mario.backend.common.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpClientService {

    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public JsonNode get(String url) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return execute(request, url);
    }

    public JsonNode post(String url, Map<String, Object> body) {
        Request request = new Request.Builder()
                .url(url)
                .post(buildRequestBody(body, url))
                .build();
        return execute(request, url);
    }

    public JsonNode put(String url, Map<String, Object> body) {
        Request request = new Request.Builder()
                .url(url)
                .put(buildRequestBody(body, url))
                .build();
        return execute(request, url);
    }

    public JsonNode delete(String url, Map<String, Object> body) {
        Request request = new Request.Builder()
                .url(url)
                .delete(buildRequestBody(body, url))
                .build();
        return execute(request, url);
    }

    public JsonNode delete(String url) {
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        return execute(request, url);
    }

    private RequestBody buildRequestBody(Map<String, Object> body, String url) {
        try {
            return RequestBody.create(objectMapper.writeValueAsString(body), JSON);
        } catch (IOException e) {
            throw new HttpClientException(url, "Failed to serialize request body", e);
        }
    }

    private JsonNode execute(Request request, String url) {
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            return objectMapper.readTree(responseBody);
        } catch (IOException e) {
            throw new HttpClientException(url, "HTTP request failed: " + e.getMessage(), e);
        }
    }
}
