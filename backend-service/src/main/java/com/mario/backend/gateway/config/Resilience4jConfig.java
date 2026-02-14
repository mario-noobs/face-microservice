package com.mario.backend.gateway.config;

import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class Resilience4jConfig {

    private final RetryRegistry retryRegistry;

    public Resilience4jConfig(RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
    }

    @PostConstruct
    public void registerRetryEventListeners() {
        retryRegistry.getAllRetries().forEach(retry -> {
            retry.getEventPublisher()
                    .onRetry(event -> log.warn("Retry attempt #{} for '{}', waiting {}ms. Last exception: {}",
                            event.getNumberOfRetryAttempts(),
                            event.getName(),
                            event.getWaitInterval().toMillis(),
                            event.getLastThrowable().getMessage()))
                    .onError(event -> log.error("Retry exhausted for '{}' after {} attempts. Last exception: {}",
                            event.getName(),
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getMessage()))
                    .onSuccess(event -> log.info("Retry succeeded for '{}' after {} attempt(s)",
                            event.getName(),
                            event.getNumberOfRetryAttempts()));
        });
    }
}
