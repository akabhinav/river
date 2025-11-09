# Connector Development Guide

This guide explains how to develop custom connectors for the River Platform.

## Table of Contents

1. [Overview](#overview)
2. [Connector Types](#connector-types)
3. [Creating a Source Connector](#creating-a-source-connector)
4. [Creating a Destination Connector](#creating-a-destination-connector)
5. [Configuration](#configuration)
6. [Best Practices](#best-practices)
7. [Testing](#testing)

## Overview

River Platform uses a plugin-based connector architecture. Connectors are Java classes that implement either the `SourceConnector` or `DestinationConnector` interface.

### Key Concepts

- **Connector**: A plugin that extracts or loads data
- **Configuration**: Type-safe configuration for the connector
- **Context**: Runtime execution context
- **Record**: The fundamental unit of data
- **Schema**: Metadata describing the structure of data

## Connector Types

### Source Connectors

Extract data from external systems and convert it into River Records.

**Interface**: `com.river.core.connector.SourceConnector<C>`

**Key Methods**:
- `configure(Map<String, Object> config)` - Configure the connector
- `start(SourceContext context)` - Initialize resources
- `read()` - Return a Stream of Records
- `stop()` - Cleanup resources

### Destination Connectors

Load River Records into external systems.

**Interface**: `com.river.core.connector.DestinationConnector<C>`

**Key Methods**:
- `configure(Map<String, Object> config)` - Configure the connector
- `start(DestinationContext context)` - Initialize resources
- `write(Stream<Record> records)` - Write records to destination
- `stop()` - Cleanup resources

## Creating a Source Connector

### Step 1: Define Configuration Class

```java
package com.example.connector;

import com.river.core.connector.ConnectorConfig;
import com.river.core.connector.ConnectorException;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MySourceConfig extends ConnectorConfig {

    private String endpoint;
    private String apiKey;
    private Integer batchSize = 1000;

    @Override
    public void validate() throws ConnectorException {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new ConnectorException(
                ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                "Endpoint is required"
            );
        }
    }
}
```

### Step 2: Implement Source Connector

```java
package com.example.connector;

import com.river.core.connector.SourceConnector;
import com.river.core.connector.ConnectorException;
import com.river.core.connector.ConnectorStats;
import com.river.core.context.SourceContext;
import com.river.core.model.Record;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class MySourceConnector implements SourceConnector<MySourceConfig> {

    private MySourceConfig config;
    private SourceContext context;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        this.config = new MySourceConfig();

        // Parse configuration
        config.setEndpoint((String) configMap.get("endpoint"));
        config.setApiKey((String) configMap.get("apiKey"));

        config.validate();
        this.stats = ConnectorStats.builder().build();
    }

    @Override
    public void start(SourceContext context) throws ConnectorException {
        log.info("Starting MySourceConnector");
        this.context = context;

        // Initialize connections, authenticate, etc.
    }

    @Override
    public Stream<Record> read() throws ConnectorException {
        log.info("Reading data from source");

        // Return a stream of records
        return fetchData().stream()
            .map(this::convertToRecord)
            .peek(record -> stats.incrementRecordsProcessed(1));
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping MySourceConnector");
        stats.complete();

        // Cleanup resources
    }

    @Override
    public String getName() {
        return "my-source";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.SOURCE;
    }

    @Override
    public Class<MySourceConfig> getConfigClass() {
        return MySourceConfig.class;
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }

    // Helper methods
    private List<?> fetchData() {
        // Implement data fetching logic
        return Collections.emptyList();
    }

    private Record convertToRecord(Object data) {
        // Convert your data to River Record
        return Record.builder()
            .schema(createSchema())
            .data(extractData(data))
            .build();
    }
}
```

## Creating a Destination Connector

### Step 1: Define Configuration Class

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class MyDestinationConfig extends ConnectorConfig {

    private String endpoint;
    private String apiKey;
    private WriteMode writeMode = WriteMode.APPEND;

    @Override
    public void validate() throws ConnectorException {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new ConnectorException(
                ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                "Endpoint is required"
            );
        }
    }
}
```

### Step 2: Implement Destination Connector

```java
@Slf4j
public class MyDestinationConnector implements DestinationConnector<MyDestinationConfig> {

    private MyDestinationConfig config;
    private DestinationContext context;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        this.config = new MyDestinationConfig();

        config.setEndpoint((String) configMap.get("endpoint"));
        config.setApiKey((String) configMap.get("apiKey"));

        config.validate();
        this.stats = ConnectorStats.builder().build();
    }

    @Override
    public void start(DestinationContext context) throws ConnectorException {
        log.info("Starting MyDestinationConnector");
        this.context = context;

        // Initialize connections
    }

    @Override
    public void write(Stream<Record> records) throws ConnectorException {
        log.info("Writing data to destination");

        List<Record> batch = new ArrayList<>();
        int batchSize = context.getBatchSize();

        records.forEach(record -> {
            batch.add(record);
            if (batch.size() >= batchSize) {
                writeBatch(new ArrayList<>(batch));
                batch.clear();
            }
        });

        // Write remaining records
        if (!batch.isEmpty()) {
            writeBatch(batch);
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping MyDestinationConnector");
        stats.complete();
    }

    @Override
    public String getName() {
        return "my-destination";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.DESTINATION;
    }

    @Override
    public Class<MyDestinationConfig> getConfigClass() {
        return MyDestinationConfig.class;
    }

    private void writeBatch(List<Record> batch) {
        // Implement batch write logic
        batch.forEach(record -> {
            // Write record
            stats.incrementRecordsProcessed(1);
        });
    }
}
```

## Configuration

### Type-Safe Configuration

Always extend `ConnectorConfig` and implement the `validate()` method:

```java
@Override
public void validate() throws ConnectorException {
    if (requiredField == null) {
        throw new ConnectorException(
            ConnectorException.ErrorCode.CONFIGURATION_ERROR,
            "Required field is missing"
        );
    }
}
```

### Configuration Properties

Use the `properties` map for additional configuration:

```java
Object value = config.getProperty("custom.property");
config.setProperty("custom.property", value);
```

## Best Practices

### 1. Error Handling

Always use appropriate error codes:

```java
throw new ConnectorException(
    ConnectorException.ErrorCode.CONNECTION_ERROR,
    "Failed to connect",
    exception
);
```

### 2. Resource Management

Clean up resources in the `stop()` method:

```java
@Override
public void stop() throws ConnectorException {
    try {
        if (connection != null) {
            connection.close();
        }
    } finally {
        stats.complete();
    }
}
```

### 3. Streaming

Use Java Streams for efficient memory usage:

```java
@Override
public Stream<Record> read() {
    return dataSource.stream()
        .map(this::convertToRecord)
        .peek(r -> stats.incrementRecordsProcessed(1));
}
```

### 4. Logging

Use SLF4J for logging:

```java
@Slf4j
public class MyConnector {
    log.info("Processing {} records", count);
    log.error("Error processing record", exception);
}
```

### 5. Statistics

Track connector statistics:

```java
stats.incrementRecordsProcessed(count);
stats.incrementBytesProcessed(bytes);
stats.incrementRecordsFailed(failedCount);
```

## Testing

### Unit Testing

```java
@Test
void testConnectorConfiguration() throws ConnectorException {
    MySourceConnector connector = new MySourceConnector();

    Map<String, Object> config = Map.of(
        "endpoint", "http://example.com",
        "apiKey", "test-key"
    );

    connector.configure(config);
    assertNotNull(connector.getConfigClass());
}
```

### Integration Testing

Use TestContainers for integration tests:

```java
@Testcontainers
class PostgresConnectorIntegrationTest {

    @Container
    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Test
    void testReadFromPostgres() {
        // Test connector with real database
    }
}
```

## Registration

Register your connector with the ConnectorRegistry:

```java
ConnectorRegistry registry = new ConnectorRegistry();
registry.registerSource("my-source", MySourceConnector.class);
registry.registerDestination("my-dest", MyDestinationConnector.class);
```

## Example Connectors

See the following implementations for reference:

- `PostgresSourceConnector` - JDBC-based source
- `S3SourceConnector` - Cloud storage source
- `PostgresDestinationConnector` - JDBC-based destination

## Next Steps

- Review the [Architecture Guide](architecture.md)
- Explore [API Documentation](api.md)
- Check [Deployment Guide](deployment.md)
