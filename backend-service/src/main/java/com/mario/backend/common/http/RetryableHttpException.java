package com.mario.backend.common.http;

import lombok.Getter;

@Getter
public class RetryableHttpException extends HttpClientException {

    private final int httpStatusCode;

    public RetryableHttpException(String url, String message, int httpStatusCode) {
        super(url, message);
        this.httpStatusCode = httpStatusCode;
    }

    public RetryableHttpException(String url, String message, Throwable cause) {
        super(url, message, cause);
        this.httpStatusCode = 0;
    }
}
