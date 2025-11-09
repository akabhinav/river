package com.river.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a data pipeline
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pipeline {

    /**
     * Unique pipeline ID
     */
    private String id;

    /**
     * Pipeline name
     */
    private String name;

    /**
     * Pipeline description
     */
    private String description;

    /**
     * Source configuration
     */
    private SourceConfig source;

    /**
     * Destination configuration
     */
    private DestinationConfig destination;

    /**
     * Transformation steps
     */
    @Builder.Default
    private List<TransformationStep> transformations = new ArrayList<>();

    /**
     * Pipeline schedule (cron expression)
     */
    private String schedule;

    /**
     * Pipeline mode
     */
    @Builder.Default
    private PipelineMode mode = PipelineMode.BATCH;

    /**
     * Pipeline state
     */
    @Builder.Default
    private PipelineState state = PipelineState.CREATED;

    /**
     * Pipeline configuration
     */
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    /**
     * Created timestamp
     */
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Updated timestamp
     */
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * Owner/creator
     */
    private String owner;

    /**
     * Tags
     */
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /**
     * Pipeline execution modes
     */
    public enum PipelineMode {
        BATCH,          // Scheduled batch processing
        STREAMING,      // Continuous streaming
        MICRO_BATCH,    // Small batches
        CDC             // Change data capture
    }

    /**
     * Pipeline states
     */
    public enum PipelineState {
        CREATED,        // Pipeline created but not started
        RUNNING,        // Pipeline is running
        PAUSED,         // Pipeline is paused
        STOPPED,        // Pipeline is stopped
        FAILED,         // Pipeline failed
        COMPLETED       // Pipeline completed successfully
    }
}
