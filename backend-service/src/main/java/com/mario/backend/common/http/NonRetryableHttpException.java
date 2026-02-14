package com.mario.backend.common.http;

import lombok.Getter;

@Getter
public class NonRetryableHttpException extends HttpClientException {

    private final int httpStatusCode;
    private final String responseBody;

    public NonRetryableHttpException(String url, String message, int httpStatusCode, String responseBody) {
        super(url, message);
        this.httpStatusCode = httpStatusCode;
        this.responseBody = responseBody;
    }
}
