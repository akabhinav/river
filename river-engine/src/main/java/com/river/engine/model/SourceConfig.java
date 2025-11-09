package com.river.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a data source
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceConfig {

    /**
     * Connector type (e.g., "postgresql", "mongodb", "kafka")
     */
    private String type;

    /**
     * Connection properties
     */
    @Builder.Default
    private Map<String, Object> connection = new HashMap<>();

    /**
     * Read mode
     */
    @Builder.Default
    private ReadMode readMode = ReadMode.FULL;

    /**
     * Incremental column (for incremental reads)
     */
    private String incrementalColumn;

    /**
     * Last checkpoint value
     */
    private Object lastCheckpoint;

    /**
     * Parallelism (number of partitions)
     */
    @Builder.Default
    private Integer parallelism = 1;

    /**
     * Additional configuration
     */
    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Read modes
     */
    public enum ReadMode {
        FULL,           // Read entire dataset
        INCREMENTAL,    // Read only new/changed data
        CDC,            // Change data capture
        SNAPSHOT        // One-time snapshot
    }
}
