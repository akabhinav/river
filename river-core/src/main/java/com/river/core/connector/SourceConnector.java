package com.river.core.connector;

import com.river.core.context.SourceContext;
import com.river.core.model.Record;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Base interface for all source connectors.
 * Source connectors extract data from external systems.
 *
 * @param <C> Configuration type for this connector
 */
public interface SourceConnector<C extends ConnectorConfig> extends Connector {

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
    void start(SourceContext context) throws ConnectorException;

    /**
     * Read data from the source.
     * This method returns a stream of records that will be processed.
     *
     * @return Stream of records from the source
     * @throws ConnectorException if reading fails
     */
    Stream<Record> read() throws ConnectorException;

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
     * Check if this connector supports incremental reads
     *
     * @return true if incremental reads are supported
     */
    default boolean supportsIncremental() {
        return false;
    }

    /**
     * Check if this connector supports CDC
     *
     * @return true if CDC is supported
     */
    default boolean supportsCDC() {
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
}
