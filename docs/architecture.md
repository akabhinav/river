# River Platform Architecture

## Table of Contents
1. [System Overview](#system-overview)
2. [Core Components](#core-components)
3. [Data Flow](#data-flow)
4. [Processing Modes](#processing-modes)
5. [Connector Architecture](#connector-architecture)
6. [Deployment Architecture](#deployment-architecture)

## System Overview

River Platform is built on a modular, plugin-based architecture that separates concerns and enables horizontal scalability.

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         River Platform                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐  │
│  │   REST API   │      │   Scheduler  │      │  Monitoring   │  │
│  └──────┬───────┘      └──────┬───────┘      └──────┬────────┘  │
│         │                     │                     │            │
│  ┌──────┴──────────────────────┴─────────────────────┴────────┐ │
│  │              Pipeline Engine (Orchestration)               │ │
│  └──────┬──────────────────────┬──────────────────────┬────────┘ │
│         │                      │                      │           │
│  ┌──────┴────────┐   ┌─────────┴────────┐   ┌────────┴───────┐  │
│  │ Batch Engine  │   │ Stream Engine    │   │  CDC Engine    │  │
│  │ (Spark)       │   │ (Flink/Kafka)    │   │  (Debezium)    │  │
│  └──────┬────────┘   └─────────┬────────┘   └────────┬───────┘  │
│         │                      │                      │           │
│  ┌──────┴──────────────────────┴──────────────────────┴────────┐ │
│  │                 Transformation Layer                        │ │
│  └──────┬──────────────────────┬──────────────────────┬────────┘ │
│         │                      │                      │           │
│  ┌──────┴────────┐   ┌─────────┴────────┐   ┌────────┴───────┐  │
│  │ Source        │   │ Schema           │   │ Destination    │  │
│  │ Connectors    │   │ Registry         │   │ Connectors     │  │
│  └───────────────┘   └──────────────────┘   └────────────────┘  │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │         Metadata Store (Lineage, Stats, Config)           │  │
│  └────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. Connector SDK (river-core)

The foundation of the platform providing:

- **Abstract Connector Interface**: Base classes for all connectors
- **Data Model**: Common data structures (Record, Schema, Field)
- **Configuration Framework**: Type-safe configuration with validation
- **Execution Context**: Runtime context for connector execution
- **Error Handling**: Standardized error handling and retry mechanisms

**Key Abstractions:**

```java
public interface SourceConnector<T> {
    void configure(Map<String, Object> config);
    void start(SourceContext context);
    Stream<Record> read();
    void stop();
}

public interface DestinationConnector<T> {
    void configure(Map<String, Object> config);
    void start(DestinationContext context);
    void write(Stream<Record> records);
    void stop();
}
```

### 2. Pipeline Engine (river-engine)

Orchestrates data movement workflows:

- **Pipeline Definition**: DAG-based pipeline configuration
- **Task Scheduling**: Cron-based and event-driven scheduling
- **State Management**: Track pipeline execution state
- **Failure Handling**: Retry, skip, or fail on errors
- **Parallelism**: Parallel task execution
- **Checkpointing**: Save/restore pipeline state

**Pipeline Lifecycle:**
1. Parse pipeline configuration
2. Validate source and destination
3. Initialize connectors
4. Execute data movement
5. Apply transformations
6. Handle errors and retries
7. Update metadata and metrics

### 3. Schema Registry (river-schema)

Centralized schema management:

- **Schema Versioning**: Track schema evolution
- **Compatibility Checking**: Forward/backward/full compatibility
- **Multiple Formats**: Avro, Parquet, JSON Schema, Protobuf
- **Schema Inference**: Auto-detect schemas from data
- **Registry API**: REST API for schema operations

### 4. Metadata Store (river-metadata)

PostgreSQL-backed metadata repository:

- **Pipeline Metadata**: Configuration, schedule, owners
- **Execution History**: Run logs, duration, status
- **Data Lineage**: Track data flow across systems
- **Statistics**: Row counts, bytes processed, errors
- **Connector Registry**: Available connectors and versions

### 5. Transformation Layer (river-transform)

In-flight data transformation:

- **Field Mapping**: Rename, add, remove fields
- **Type Conversion**: Cast between data types
- **Filtering**: Row-level filtering with predicates
- **Aggregation**: Group by and aggregate functions
- **Enrichment**: Lookup and join operations
- **Custom UDFs**: User-defined transformation functions

### 6. Security Layer (river-security)

Enterprise security features:

- **Authentication**: LDAP, OAuth2, API keys
- **Authorization**: RBAC for pipelines and connectors
- **Encryption**: Data encryption at rest and in transit
- **Credential Management**: Secure storage of credentials (Vault integration)
- **Audit Logging**: Track all operations

### 7. Monitoring & Observability (river-monitoring)

Production monitoring:

- **Metrics**: Prometheus metrics for throughput, latency, errors
- **Logs**: Structured logging with ELK integration
- **Tracing**: Distributed tracing with OpenTelemetry
- **Alerting**: Alert on failures, SLA violations
- **Dashboards**: Grafana dashboards for visualization

## Data Flow

### Typical Data Movement Flow

```
Source System
    ↓
Source Connector (Extract)
    ↓
Schema Registry (Validate)
    ↓
Transformation Layer (Transform)
    ↓
Processing Engine (Batch/Stream)
    ↓
Destination Connector (Load)
    ↓
Destination System
    ↓
Metadata Store (Track)
```

### Record Model

All data flows through a common `Record` abstraction:

```java
public class Record {
    private Schema schema;
    private Map<String, Object> data;
    private Map<String, String> metadata;
    private long timestamp;
}
```

## Processing Modes

### 1. Batch Processing (Apache Spark)

- **Use Case**: Large volume data movement, historical data migration
- **Characteristics**: High throughput, eventual consistency
- **Execution**: Scheduled or manual trigger
- **Parallelism**: Distributed across Spark cluster

### 2. Streaming Processing (Apache Flink)

- **Use Case**: Real-time data synchronization, event processing
- **Characteristics**: Low latency, exactly-once semantics
- **Execution**: Continuous processing
- **Parallelism**: Parallel stream operators

### 3. Micro-Batch

- **Use Case**: Near real-time with small batches
- **Characteristics**: Balance between latency and throughput
- **Execution**: Fixed time intervals (e.g., every 5 seconds)

### 4. Change Data Capture (CDC)

- **Use Case**: Database replication, real-time sync
- **Characteristics**: Transaction log-based, low overhead
- **Execution**: Continuous change stream processing

## Connector Architecture

### Connector Lifecycle

1. **Registration**: Connector registered in metadata store
2. **Configuration**: User provides configuration
3. **Validation**: Validate connectivity and permissions
4. **Initialization**: Allocate resources, establish connections
5. **Execution**: Read/write data
6. **Checkpointing**: Save progress periodically
7. **Cleanup**: Release resources, close connections

### Connector Types

#### Source Connectors

- **Full Table Scan**: Read entire dataset
- **Incremental**: Read only new/changed data
- **CDC**: Capture database changes
- **Streaming**: Consume from message queues

#### Destination Connectors

- **Batch Insert**: Bulk load operations
- **Upsert**: Insert or update based on key
- **Append**: Append-only writes
- **Merge**: Complex merge logic

### Plugin Architecture

Connectors are loaded dynamically:

```java
ConnectorRegistry registry = new ConnectorRegistry();
registry.register("postgresql-source", PostgresSourceConnector.class);

SourceConnector connector = registry.getSource("postgresql-source");
```

## Deployment Architecture

### Standalone Mode

Single JVM deployment for development:

```
┌─────────────────────┐
│   River Runtime     │
│  ┌───────────────┐  │
│  │ All Components│  │
│  └───────────────┘  │
└─────────────────────┘
```

### Distributed Mode

Multi-node cluster for production:

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Master     │  │   Worker 1   │  │   Worker 2   │
│ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │
│ │Scheduler │ │  │ │Executors │ │  │ │Executors │ │
│ │Metadata  │ │  │ │          │ │  │ │          │ │
│ └──────────┘ │  │ └──────────┘ │  │ └──────────┘ │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       └─────────────────┴─────────────────┘
                    Coordination
```

### Kubernetes Deployment

Cloud-native deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: river-master
spec:
  replicas: 1
  template:
    spec:
      containers:
      - name: river
        image: river:latest
        env:
        - name: RIVER_MODE
          value: "master"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: river-worker
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: river
        image: river:latest
        env:
        - name: RIVER_MODE
          value: "worker"
```

## Design Principles

1. **Modularity**: Each component is independently deployable
2. **Extensibility**: Plugin architecture for connectors
3. **Scalability**: Horizontal scaling with distributed processing
4. **Reliability**: Fault tolerance with retries and checkpointing
5. **Observability**: Comprehensive monitoring and logging
6. **Security**: Enterprise-grade authentication and encryption
7. **Performance**: Optimized for high throughput and low latency
