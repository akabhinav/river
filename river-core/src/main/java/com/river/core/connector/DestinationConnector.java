package com.river.core.connector;

import com.river.core.context.DestinationContext;
import com.river.core.model.Record;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Base interface for all destination connectors.
 * Destination connectors load data into external systems.
 *
 * @param <C> Configuration type for this connector
 */
public interface DestinationConnector<C extends ConnectorConfig> extends Connector {

    /**
     * Configure the connector with provided configuration
     *
     * @param config Configuration map
     * @throws ConnectorException if configuration is invalid
     */
    void configure(Map<String, Object> config) throws ConnectorException;

    /**
     * Start the connector with the given context
     *
     * @param context Execution context
     * @throws ConnectorException if startup fails
     */
    void start(DestinationContext context) throws ConnectorException;

    /**
     * Write records to the destination.
     * This method processes a stream of records and loads them into the destination.
     *
     * @param records Stream of records to write
     * @throws ConnectorException if writing fails
     */
    void write(Stream<Record> records) throws ConnectorException;

    /**
     * Stop the connector and release resources
     *
     * @throws ConnectorException if shutdown fails
     */
    void stop() throws ConnectorException;

    /**
     * Get the configuration class for this connector
     *
     * @return Configuration class
     */
    Class<C> getConfigClass();

    /**
     * Get the write mode for this connector
     *
     * @return Write mode
     */
    default WriteMode getWriteMode() {
        return WriteMode.APPEND;
    }

    /**
     * Check if this connector supports transactions
     *
     * @return true if transactions are supported
     */
    default boolean supportsTransactions() {
        return false;
    }

    /**
     * Get connector statistics
     *
     * @return Connector statistics
     */
    default ConnectorStats getStats() {
        return new ConnectorStats();
    }

    /**
     * Write modes for destination connectors
     */
    enum WriteMode {
        APPEND,      // Append new records
        UPSERT,      // Insert or update based on key
        OVERWRITE,   // Replace existing data
        MERGE        // Complex merge logic
    }
}
