package com.river.core.util;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * Retry policy for connector operations
 */
@Data
@Builder
public class RetryPolicy {

    /**
     * Maximum number of retries
     */
    @Builder.Default
    private int maxRetries = 3;

    /**
     * Initial retry delay
     */
    @Builder.Default
    private Duration initialDelay = Duration.ofSeconds(1);

    /**
     * Maximum retry delay
     */
    @Builder.Default
    private Duration maxDelay = Duration.ofMinutes(5);

    /**
     * Backoff multiplier
     */
    @Builder.Default
    private double backoffMultiplier = 2.0;

    /**
     * Whether to use jitter
     */
    @Builder.Default
    private boolean useJitter = true;

    /**
     * Calculate delay for a given retry attempt
     *
     * @param attempt Retry attempt number (0-based)
     * @return Delay duration
     */
    public Duration calculateDelay(int attempt) {
        long delayMillis = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, attempt));
        delayMillis = Math.min(delayMillis, maxDelay.toMillis());

        if (useJitter) {
            delayMillis = (long) (delayMillis * (0.5 + Math.random() * 0.5));
        }

        return Duration.ofMillis(delayMillis);
    }

    /**
     * Default retry policy
     */
    public static RetryPolicy defaultPolicy() {
        return RetryPolicy.builder().build();
    }

    /**
     * No retry policy
     */
    public static RetryPolicy noRetry() {
        return RetryPolicy.builder().maxRetries(0).build();
    }

    /**
     * Aggressive retry policy
     */
    public static RetryPolicy aggressive() {
        return RetryPolicy.builder()
                .maxRetries(10)
                .initialDelay(Duration.ofMillis(100))
                .maxDelay(Duration.ofMinutes(1))
                .build();
    }
}
