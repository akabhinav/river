package com.river.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a data destination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationConfig {

    /**
     * Connector type (e.g., "postgresql", "mongodb", "s3")
     */
    private String type;

    /**
     * Connection properties
     */
    @Builder.Default
    private Map<String, Object> connection = new HashMap<>();

    /**
     * Write mode
     */
    @Builder.Default
    private WriteMode writeMode = WriteMode.APPEND;

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
     * Upsert key fields (for UPSERT mode)
     */
    private String[] upsertKeys;

    /**
     * Additional configuration
     */
    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Write modes
     */
    public enum WriteMode {
        APPEND,         // Append new records
        UPSERT,         // Insert or update
        OVERWRITE,      // Replace existing data
        MERGE           // Complex merge logic
    }
}
