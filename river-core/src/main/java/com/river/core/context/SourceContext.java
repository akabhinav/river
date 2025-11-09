package com.river.core.context;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution context for source connectors
 */
@Data
@Builder
public class SourceContext {

    /**
     * Pipeline ID
     */
    private String pipelineId;

    /**
     * Task ID
     */
    private String taskId;

    /**
     * Partition ID (for parallel execution)
     */
    private Integer partitionId;

    /**
     * Total number of partitions
     */
    private Integer totalPartitions;

    /**
     * Offset to resume from
     */
    private Long offset;

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

    /**
     * Check if running in distributed mode
     */
    public boolean isDistributed() {
        return totalPartitions != null && totalPartitions > 1;
    }
}
