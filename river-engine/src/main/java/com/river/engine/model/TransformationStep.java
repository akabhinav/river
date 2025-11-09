package com.river.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a transformation step in the pipeline
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformationStep {

    /**
     * Transformation type
     */
    private TransformationType type;

    /**
     * Transformation name
     */
    private String name;

    /**
     * Transformation configuration
     */
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();

    /**
     * Order/sequence of this transformation
     */
    private Integer order;

    /**
     * Transformation types
     */
    public enum TransformationType {
        FILTER,         // Filter records based on condition
        MAP,            // Map/rename fields
        AGGREGATE,      // Aggregate data
        JOIN,           // Join with another dataset
        ENRICH,         // Enrich with lookup data
        CONVERT,        // Type conversion
        CUSTOM          // Custom transformation
    }
}
