package com.river.core.model;

/**
 * Supported data types in River Platform
 */
public enum DataType {
    // Primitive types
    BOOLEAN,
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    DECIMAL,

    // String types
    STRING,
    TEXT,
    VARCHAR,
    CHAR,

    // Binary types
    BINARY,
    BYTES,

    // Date/Time types
    DATE,
    TIME,
    TIMESTAMP,
    TIMESTAMP_WITH_TIMEZONE,
    INTERVAL,

    // Complex types
    ARRAY,
    MAP,
    STRUCT,
    JSON,

    // Special types
    UUID,
    ENUM,
    NULL,
    UNKNOWN;

    /**
     * Check if type is numeric
     */
    public boolean isNumeric() {
        return this == BYTE || this == SHORT || this == INT ||
               this == LONG || this == FLOAT || this == DOUBLE ||
               this == DECIMAL;
    }

    /**
     * Check if type is temporal
     */
    public boolean isTemporal() {
        return this == DATE || this == TIME || this == TIMESTAMP ||
               this == TIMESTAMP_WITH_TIMEZONE || this == INTERVAL;
    }

    /**
     * Check if type is string-like
     */
    public boolean isString() {
        return this == STRING || this == TEXT || this == VARCHAR || this == CHAR;
    }

    /**
     * Check if type is complex
     */
    public boolean isComplex() {
        return this == ARRAY || this == MAP || this == STRUCT || this == JSON;
    }
}
