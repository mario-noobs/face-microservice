package com.mario.backend.common.http;

import lombok.Getter;

@Getter
public class HttpClientException extends RuntimeException {

    private final String url;

    public HttpClientException(String url, String message, Throwable cause) {
        super(message, cause);
        this.url = url;
    }

    public HttpClientException(String url, String message) {
        super(message);
        this.url = url;
    }
}
