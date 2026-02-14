package com.mario.backend.common.http;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpClientService {

    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient okHttpClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Retry(name = "externalService", fallbackMethod = "handleRetryExhausted")
    public String get(String url) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return execute(request, url);
    }

    @Retry(name = "externalService", fallbackMethod = "handleRetryExhaustedWithBody")
    public String post(String url, Map<String, Object> body) {
        Request request = new Request.Builder()
                .url(url)
                .post(buildRequestBody(body, url))
                .build();
        return execute(request, url);
    }

    @Retry(name = "externalService", fallbackMethod = "handleRetryExhaustedWithBody")
    public String put(String url, Map<String, Object> body) {
        Request request = new Request.Builder()
                .url(url)
                .put(buildRequestBody(body, url))
                .build();
        return execute(request, url);
    }

    @Retry(name = "externalService", fallbackMethod = "handleRetryExhaustedWithBody")
    public String delete(String url, Map<String, Object> body) {
        Request request = new Request.Builder()
                .url(url)
                .delete(buildRequestBody(body, url))
                .build();
        return execute(request, url);
    }

    @Retry(name = "externalService", fallbackMethod = "handleRetryExhausted")
    public String delete(String url) {
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

    private String execute(Request request, String url) {
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            int statusCode = response.code();

            if (response.isSuccessful()) {
                return responseBody;
            }

            if (statusCode == 429 || statusCode >= 500) {
                throw new RetryableHttpException(url,
                        "HTTP " + statusCode + ": " + responseBody, statusCode);
            }

            throw new NonRetryableHttpException(url,
                    "HTTP " + statusCode + ": " + responseBody, statusCode, responseBody);

        } catch (RetryableHttpException | NonRetryableHttpException e) {
            throw e;
        } catch (SocketTimeoutException e) {
            throw new RetryableHttpException(url, "Request timed out", e);
        } catch (ConnectException e) {
            throw new RetryableHttpException(url, "Connection failed", e);
        } catch (UnknownHostException e) {
            throw new RetryableHttpException(url, "DNS resolution failed", e);
        } catch (IOException e) {
            throw new RetryableHttpException(url, "HTTP request failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    private String handleRetryExhausted(String url, Exception ex) {
        log.error("Retry exhausted for GET/DELETE {}: {}", url, ex.getMessage());
        throw new HttpClientException(url, "External service unavailable after retries: " + ex.getMessage(), ex);
    }

    @SuppressWarnings("unused")
    private String handleRetryExhaustedWithBody(String url, Map<String, Object> body, Exception ex) {
        log.error("Retry exhausted for {}: {}", url, ex.getMessage());
        throw new HttpClientException(url, "External service unavailable after retries: " + ex.getMessage(), ex);
    }
}
