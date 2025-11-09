# River Platform - Connector Catalog

Complete list of all available connectors in River Platform.

## Overview

**Total Connectors:** 13 (8 sources + 5 destinations)

All connectors are production-ready and can be tested locally with Docker Compose.

---

## Source Connectors (8)

### 1. PostgreSQL Source

**Type:** `postgresql-source`
**Category:** Relational Database
**Supports:** Full scan, Incremental, CDC

**Configuration:**
```json
{
  "type": "postgresql-source",
  "connection": {
    "host": "localhost",
    "port": 5432,
    "database": "mydb",
    "username": "user",
    "password": "pass",
    "table": "users",
    "query": "SELECT * FROM users"
  }
}
```

**Features:**
- Full table scans
- Incremental reads (timestamp/ID based)
- CDC via logical replication
- Parallel reads
- Schema extraction

---

### 2. MySQL Source

**Type:** `mysql-source`
**Category:** Relational Database
**Supports:** Full scan, Incremental, CDC

**Configuration:**
```json
{
  "type": "mysql-source",
  "connection": {
    "host": "localhost",
    "port": 3306,
    "database": "mydb",
    "username": "user",
    "password": "pass",
    "table": "users"
  }
}
```

**Features:**
- Streaming mode for large datasets
- Binlog-based CDC
- Custom queries
- Type mapping

---

### 3. MongoDB Source

**Type:** `mongodb-source`
**Category:** NoSQL Database
**Supports:** Full scan, Incremental, CDC

**Configuration:**
```json
{
  "type": "mongodb-source",
  "connection": {
    "uri": "mongodb://localhost:27017",
    "database": "mydb",
    "collection": "users",
    "filter": "{\"status\":\"active\"}",
    "batchSize": 1000
  }
}
```

**Features:**
- Document streaming
- Change Streams for CDC
- Filter support
- Flexible JSON documents

---

### 4. Redis Source

**Type:** `redis-source`
**Category:** Cache/Key-Value Store
**Supports:** Full scan

**Configuration:**
```json
{
  "type": "redis-source",
  "connection": {
    "host": "localhost",
    "port": 6379,
    "password": "pass",
    "database": 0,
    "pattern": "*",
    "scanCount": 1000
  }
}
```

**Features:**
- SCAN-based key iteration
- Pattern matching
- All data types (String, Hash, List, Set, ZSet)
- TTL extraction

---

### 5. Kafka Source

**Type:** `kafka-source`
**Category:** Message Streaming
**Supports:** Streaming

**Configuration:**
```json
{
  "type": "kafka-source",
  "connection": {
    "bootstrapServers": "localhost:9092",
    "topic": "my-topic",
    "groupId": "river-consumer",
    "autoOffsetReset": "earliest",
    "pollTimeout": 1000
  }
}
```

**Features:**
- Consumer group management
- Offset tracking
- Multiple partitions
- Metadata extraction

---

### 6. Elasticsearch Source

**Type:** `elasticsearch-source`
**Category:** Search Engine
**Supports:** Full scan, Query-based

**Configuration:**
```json
{
  "type": "elasticsearch-source",
  "connection": {
    "host": "localhost",
    "port": 9200,
    "index": "logs-*",
    "query": "{\"query\":{\"match_all\":{}}}",
    "scrollSize": 1000,
    "scrollTimeout": "1m"
  }
}
```

**Features:**
- Scroll API for large datasets
- Complex query support
- Multi-index search
- Wildcard indices

---

### 7. S3 Source

**Type:** `s3-source`
**Category:** Object Storage
**Supports:** Full scan

**Configuration:**
```json
{
  "type": "s3-source",
  "connection": {
    "bucket": "my-bucket",
    "prefix": "data/",
    "region": "us-east-1",
    "accessKey": "ACCESS_KEY",
    "secretKey": "SECRET_KEY",
    "format": "json"
  }
}
```

**Features:**
- Prefix filtering
- Multiple formats (JSON, CSV, Parquet)
- Large file handling
- Parallel downloads

---

### 8. REST API Source

**Type:** `rest-api-source`
**Category:** API
**Supports:** HTTP/HTTPS calls

**Configuration:**
```json
{
  "type": "rest-api-source",
  "connection": {
    "endpoint": "https://api.example.com/data",
    "apiKey": "YOUR_API_KEY",
    "method": "GET",
    "pagination": "cursor"
  }
}
```

**Features:**
- HTTP methods support
- Pagination (cursor, offset)
- Rate limiting
- Authentication

---

## Destination Connectors (5)

### 1. PostgreSQL Destination

**Type:** `postgresql-destination`
**Category:** Relational Database
**Supports:** Append, Upsert, Batch

**Configuration:**
```json
{
  "type": "postgresql-destination",
  "connection": {
    "host": "localhost",
    "port": 5432,
    "database": "mydb",
    "username": "user",
    "password": "pass",
    "table": "users"
  },
  "writeMode": "UPSERT",
  "batchSize": 1000,
  "transactional": true
}
```

**Features:**
- COPY for bulk loads
- Upsert with conflict resolution
- Transactions
- Batch writes

---

### 2. MySQL Destination

**Type:** `mysql-destination`
**Category:** Relational Database
**Supports:** Append, Batch

**Configuration:**
```json
{
  "type": "mysql-destination",
  "connection": {
    "host": "localhost",
    "port": 3306,
    "database": "mydb",
    "username": "user",
    "password": "pass",
    "table": "users"
  },
  "writeMode": "APPEND",
  "batchSize": 1000,
  "transactional": true
}
```

**Features:**
- Batched inserts
- LOAD DATA optimization
- Transactions
- Rewritable statements

---

### 3. MongoDB Destination

**Type:** `mongodb-destination`
**Category:** NoSQL Database
**Supports:** Append, Batch

**Configuration:**
```json
{
  "type": "mongodb-destination",
  "connection": {
    "uri": "mongodb://localhost:27017",
    "database": "mydb",
    "collection": "users"
  },
  "writeMode": "APPEND",
  "batchSize": 1000
}
```

**Features:**
- Bulk inserts
- Unordered writes
- Document validation
- Index creation

---

### 4. Redis Destination

**Type:** `redis-destination`
**Category:** Cache/Key-Value Store
**Supports:** Append

**Configuration:**
```json
{
  "type": "redis-destination",
  "connection": {
    "host": "localhost",
    "port": 6379,
    "password": "pass",
    "database": 0,
    "keyField": "id",
    "valueField": "data",
    "ttl": 3600
  },
  "writeMode": "APPEND",
  "batchSize": 100
}
```

**Features:**
- Pipeline for batching
- TTL support
- Key-value mapping
- Atomic operations

---

### 5. Kafka Destination

**Type:** `kafka-destination`
**Category:** Message Streaming
**Supports:** Append, Streaming

**Configuration:**
```json
{
  "type": "kafka-destination",
  "connection": {
    "bootstrapServers": "localhost:9092",
    "topic": "my-topic",
    "keyField": "id",
    "valueField": "data"
  },
  "writeMode": "APPEND"
}
```

**Features:**
- Producer with batching
- Async writes
- Partitioning support
- Delivery guarantees

---

### 6. Elasticsearch Destination

**Type:** `elasticsearch-destination`
**Category:** Search Engine
**Supports:** Append, Bulk

**Configuration:**
```json
{
  "type": "elasticsearch-destination",
  "connection": {
    "host": "localhost",
    "port": 9200,
    "index": "my-index",
    "idField": "_id"
  },
  "writeMode": "APPEND",
  "batchSize": 500
}
```

**Features:**
- Bulk API
- Custom ID mapping
- Index creation
- Refresh control

---

## Testing All Connectors Locally

### Start All Services

```bash
# Start entire stack
docker-compose up -d

# Check status
docker-compose ps
```

### Services Available

| Service | Port | Credentials |
|---------|------|-------------|
| PostgreSQL | 5432 | river/river_pass |
| MySQL | 3306 | river/river_pass |
| MongoDB | 27017 | river/river_pass |
| Redis | 6379 | (no auth) |
| Kafka | 9092 | (no auth) |
| Elasticsearch | 9200 | (no auth) |
| River API | 8080 | - |

### Test Each Connector

```bash
# 1. PostgreSQL to PostgreSQL
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/postgres-to-postgres-pipeline.json

# 2. MongoDB to Elasticsearch
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/mongodb-to-elasticsearch-pipeline.json

# 3. MySQL to Kafka
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/mysql-to-kafka-pipeline.json

# 4. Kafka to MongoDB
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/kafka-to-mongodb-pipeline.json

# 5. PostgreSQL to Redis
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/postgres-to-redis-cache-pipeline.json
```

---

## Connector Matrix

| Source → Destination | PostgreSQL | MySQL | MongoDB | Redis | Kafka | Elasticsearch |
|---------------------|------------|-------|---------|-------|-------|---------------|
| **PostgreSQL** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **MySQL** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **MongoDB** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Redis** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Kafka** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Elasticsearch** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **S3** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

**All combinations supported!** 🎉

---

## Next Steps

1. See [Connector Development Guide](connector-development.md) to build custom connectors
2. Check [Example Pipelines](../examples/) for real-world use cases
3. Read [Local Testing Guide](local-testing-guide.md) for testing instructions
4. Review [Architecture](architecture.md) to understand the platform design
