package com.changha.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(
        boolean autoDispatchEnabled,
        Retry retry,
        Recovery recovery,
        Executor executor
) {

    public record Retry(int maxAttempts, long baseDelaySeconds) {
    }

    public record Recovery(int batchSize, long leaseTimeoutSeconds) {
    }

    public record Executor(String type) {
    }
}
