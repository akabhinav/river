package com.river.engine.executor;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

/**
 * Result of pipeline execution
 */
@Data
@Builder
public class PipelineExecutionResult {

    private String pipelineId;
    private String pipelineName;

    @Builder.Default
    private PipelineExecutor.ExecutionStatus status = PipelineExecutor.ExecutionStatus.RUNNING;

    @Builder.Default
    private Instant startTime = Instant.now();

    private Instant endTime;

    @Builder.Default
    private long recordsProcessed = 0;

    @Builder.Default
    private long bytesProcessed = 0;

    @Builder.Default
    private long recordsFailed = 0;

    private String errorMessage;

    /**
     * Mark execution as complete
     */
    public void complete() {
        this.endTime = Instant.now();
    }

    /**
     * Get execution duration
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
}
