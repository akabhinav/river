package com.river.core.connector;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for connector configurations
 */
@Data
public abstract class ConnectorConfig {

    /**
     * Connector name
     */
    @NotNull
    private String name;

    /**
     * Connector type
     */
    @NotNull
    private String type;

    /**
     * Additional properties
     */
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Validate the configuration
     *
     * @throws ConnectorException if configuration is invalid
     */
    public abstract void validate() throws ConnectorException;

    /**
     * Get a property value
     *
     * @param key Property key
     * @return Property value
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Get a property value with default
     *
     * @param key Property key
     * @param defaultValue Default value if key not found
     * @return Property value or default
     */
    public Object getProperty(String key, Object defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    /**
     * Set a property
     *
     * @param key Property key
     * @param value Property value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
}
