package com.river.core.context;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution context for destination connectors
 */
@Data
@Builder
public class DestinationContext {

    /**
     * Pipeline ID
     */
    private String pipelineId;

    /**
     * Task ID
     */
    private String taskId;

    /**
     * Batch size for writes
     */
    @Builder.Default
    private Integer batchSize = 1000;

    /**
     * Enable transactions
     */
    @Builder.Default
    private Boolean transactional = false;

    /**
     * Checkpoint directory for state
     */
    private String checkpointDir;

    /**
     * Additional context properties
     */
    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Get a context property
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Set a context property
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
}
