package com.mario.backend.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests that run against a fully deployed docker-compose stack.
 * <p>
 * These tests are excluded from normal {@code ./gradlew test} runs and are
 * only executed via {@code ./gradlew e2eTest}.
 * <p>
 * Prerequisites: all services must be running (docker-compose up).
 */
@Tag("e2e")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class FullStackE2ETest {

    private final String baseUrl = System.getProperty("e2e.base-url", "http://localhost:8080");
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String testEmail = "e2e-test-" + UUID.randomUUID() + "@test.com";
    private final String testPassword = "E2eTestPass123";

    private String accessToken;
    private String refreshToken;

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    /**
     * Parse the response body into a Jackson {@link JsonNode} for flexible
     * field access regardless of the generic type.
     */
    private JsonNode parseBody(ResponseEntity<String> response) throws Exception {
        assertThat(response.getBody()).isNotNull();
        return objectMapper.readTree(response.getBody());
    }

    /**
     * Extract access_token and refresh_token strings from a standard
     * {@code {"data": {"access_token": {"token": "..."}, "refresh_token": {"token": "..."}}}}
     * response and store them in instance fields.
     */
    private void extractAndStoreTokens(JsonNode body) {
        JsonNode data = body.get("data");
        assertThat(data).isNotNull();

        JsonNode accessTokenNode = data.get("access_token");
        assertThat(accessTokenNode).isNotNull();
        accessToken = accessTokenNode.get("token").asText();
        assertThat(accessToken).isNotBlank();

        JsonNode refreshTokenNode = data.get("refresh_token");
        assertThat(refreshTokenNode).isNotNull();
        refreshToken = refreshTokenNode.get("token").asText();
        assertThat(refreshToken).isNotBlank();
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    @Order(1)
    void healthCheck() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/ping", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = parseBody(response);
        assertThat(body.get("data").asText()).isEqualTo("pong");
    }

    @Test
    @Order(2)
    void registerUser() throws Exception {
        Map<String, String> request = Map.of(
                "firstName", "E2E",
                "lastName", "TestUser",
                "email", testEmail,
                "password", testPassword
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, jsonHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/user/register", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode body = parseBody(response);
        assertThat(body.get("error")).isNull();
        extractAndStoreTokens(body);
    }

    @Test
    @Order(3)
    void loginUser() throws Exception {
        Map<String, String> request = Map.of(
                "email", testEmail,
                "password", testPassword
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, jsonHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/user/authenticate", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = parseBody(response);
        assertThat(body.get("error")).isNull();
        extractAndStoreTokens(body);
    }

    @Test
    @Order(4)
    void getProfile() throws Exception {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/profile", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = parseBody(response);
        assertThat(body.get("error")).isNull();

        JsonNode data = body.get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("firstName").asText()).isEqualTo("E2E");
    }

    @Test
    @Order(5)
    void refreshToken() throws Exception {
        Map<String, String> request = Map.of(
                "refresh_token", refreshToken
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, jsonHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/user/refresh", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = parseBody(response);
        assertThat(body.get("error")).isNull();

        // Update the access token for subsequent tests; refresh token may also rotate
        JsonNode data = body.get("data");
        assertThat(data).isNotNull();

        JsonNode newAccessToken = data.get("access_token");
        assertThat(newAccessToken).isNotNull();
        accessToken = newAccessToken.get("token").asText();
        assertThat(accessToken).isNotBlank();

        // If the server rotates refresh tokens, capture the new one as well
        JsonNode newRefreshToken = data.get("refresh_token");
        if (newRefreshToken != null && newRefreshToken.has("token")) {
            refreshToken = newRefreshToken.get("token").asText();
        }
    }

    @Test
    @Order(6)
    void logoutUser() throws Exception {
        Map<String, String> request = Map.of(
                "access_token", accessToken
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, jsonHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/user/logout", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = parseBody(response);
        assertThat(body.get("error")).isNull();
    }
}
