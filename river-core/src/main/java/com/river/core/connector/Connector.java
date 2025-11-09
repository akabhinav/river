package com.river.core.connector;

/**
 * Base interface for all connectors (source and destination)
 */
public interface Connector {

    /**
     * Get the connector name
     *
     * @return Connector name
     */
    String getName();

    /**
     * Get the connector version
     *
     * @return Connector version
     */
    String getVersion();

    /**
     * Get the connector description
     *
     * @return Connector description
     */
    default String getDescription() {
        return "";
    }

    /**
     * Validate the connector state
     *
     * @throws ConnectorException if connector is in invalid state
     */
    default void validate() throws ConnectorException {
        // Default implementation does nothing
    }

    /**
     * Get connector type
     *
     * @return Connector type
     */
    ConnectorType getType();

    /**
     * Connector types
     */
    enum ConnectorType {
        SOURCE,
        DESTINATION
    }
}
