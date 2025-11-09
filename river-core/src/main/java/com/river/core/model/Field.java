package com.river.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a field in a schema
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Field {

    /**
     * Field name
     */
    private String name;

    /**
     * Field data type
     */
    private DataType type;

    /**
     * Is field nullable
     */
    @Builder.Default
    private boolean nullable = true;

    /**
     * Default value
     */
    private Object defaultValue;

    /**
     * Field documentation
     */
    private String documentation;

    /**
     * Is field a primary key
     */
    @Builder.Default
    private boolean primaryKey = false;

    /**
     * Field metadata
     */
    private java.util.Map<String, String> metadata;
}
