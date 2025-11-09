package com.river.core.registry;

import com.river.core.connector.Connector;
import com.river.core.connector.DestinationConnector;
import com.river.core.connector.SourceConnector;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing connector plugins
 */
@Slf4j
public class ConnectorRegistry {

    private final Map<String, Class<? extends SourceConnector>> sourceConnectors = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends DestinationConnector>> destinationConnectors = new ConcurrentHashMap<>();

    /**
     * Register a source connector
     *
     * @param name Connector name
     * @param connectorClass Connector class
     */
    public void registerSource(String name, Class<? extends SourceConnector> connectorClass) {
        log.info("Registering source connector: {}", name);
        sourceConnectors.put(name, connectorClass);
    }

    /**
     * Register a destination connector
     *
     * @param name Connector name
     * @param connectorClass Connector class
     */
    public void registerDestination(String name, Class<? extends DestinationConnector> connectorClass) {
        log.info("Registering destination connector: {}", name);
        destinationConnectors.put(name, connectorClass);
    }

    /**
     * Get a source connector instance
     *
     * @param name Connector name
     * @return Source connector instance
     * @throws ConnectorNotFoundException if connector not found
     */
    public SourceConnector getSource(String name) throws ConnectorNotFoundException {
        Class<? extends SourceConnector> connectorClass = sourceConnectors.get(name);
        if (connectorClass == null) {
            throw new ConnectorNotFoundException("Source connector not found: " + name);
        }

        try {
            return connectorClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate source connector: " + name, e);
        }
    }

    /**
     * Get a destination connector instance
     *
     * @param name Connector name
     * @return Destination connector instance
     * @throws ConnectorNotFoundException if connector not found
     */
    public DestinationConnector getDestination(String name) throws ConnectorNotFoundException {
        Class<? extends DestinationConnector> connectorClass = destinationConnectors.get(name);
        if (connectorClass == null) {
            throw new ConnectorNotFoundException("Destination connector not found: " + name);
        }

        try {
            return connectorClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate destination connector: " + name, e);
        }
    }

    /**
     * Check if source connector exists
     *
     * @param name Connector name
     * @return true if exists
     */
    public boolean hasSource(String name) {
        return sourceConnectors.containsKey(name);
    }

    /**
     * Check if destination connector exists
     *
     * @param name Connector name
     * @return true if exists
     */
    public boolean hasDestination(String name) {
        return destinationConnectors.containsKey(name);
    }

    /**
     * Get all source connector names
     *
     * @return Set of source connector names
     */
    public java.util.Set<String> getSourceConnectorNames() {
        return sourceConnectors.keySet();
    }

    /**
     * Get all destination connector names
     *
     * @return Set of destination connector names
     */
    public java.util.Set<String> getDestinationConnectorNames() {
        return destinationConnectors.keySet();
    }

    /**
     * Exception thrown when connector is not found
     */
    public static class ConnectorNotFoundException extends Exception {
        public ConnectorNotFoundException(String message) {
            super(message);
        }
    }
}
