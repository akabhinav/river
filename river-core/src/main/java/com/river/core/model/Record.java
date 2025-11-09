package com.river.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single data record in River Platform.
 * This is the fundamental unit of data that flows through the platform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Record {

    /**
     * Schema definition for this record
     */
    private Schema schema;

    /**
     * Actual data as key-value pairs
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    /**
     * Metadata about the record (source, partition, offset, etc.)
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Timestamp when the record was created/extracted
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Operation type for CDC scenarios
     */
    private OperationType operation;

    /**
     * Get a field value by name
     */
    public Object getField(String fieldName) {
        return data.get(fieldName);
    }

    /**
     * Set a field value
     */
    public void setField(String fieldName, Object value) {
        data.put(fieldName, value);
    }

    /**
     * Add metadata
     */
    public void addMetadata(String key, String value) {
        metadata.put(key, value);
    }

    /**
     * Get metadata value
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Operation types for CDC
     */
    public enum OperationType {
        INSERT,
        UPDATE,
        DELETE,
        SNAPSHOT,
        UNKNOWN
    }
}
