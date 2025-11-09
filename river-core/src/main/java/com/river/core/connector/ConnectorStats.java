package com.river.core.connector;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

/**
 * Statistics for connector execution
 */
@Data
@Builder
public class ConnectorStats {

    @Builder.Default
    private long recordsProcessed = 0;

    @Builder.Default
    private long bytesProcessed = 0;

    @Builder.Default
    private long recordsFailed = 0;

    @Builder.Default
    private Instant startTime = Instant.now();

    private Instant endTime;

    @Builder.Default
    private long currentOffset = -1;

    /**
     * Get duration of execution
     */
    public Duration getDuration() {
        if (endTime == null) {
            return Duration.between(startTime, Instant.now());
        }
        return Duration.between(startTime, endTime);
    }

    /**
     * Get throughput in records per second
     */
    public double getRecordsPerSecond() {
        long seconds = getDuration().getSeconds();
        if (seconds == 0) {
            return 0;
        }
        return (double) recordsProcessed / seconds;
    }

    /**
     * Get throughput in bytes per second
     */
    public double getBytesPerSecond() {
        long seconds = getDuration().getSeconds();
        if (seconds == 0) {
            return 0;
        }
        return (double) bytesProcessed / seconds;
    }

    /**
     * Increment records processed
     */
    public void incrementRecordsProcessed(long count) {
        this.recordsProcessed += count;
    }

    /**
     * Increment bytes processed
     */
    public void incrementBytesProcessed(long bytes) {
        this.bytesProcessed += bytes;
    }

    /**
     * Increment failed records
     */
    public void incrementRecordsFailed(long count) {
        this.recordsFailed += count;
    }

    /**
     * Mark execution as complete
     */
    public void complete() {
        this.endTime = Instant.now();
    }
}
