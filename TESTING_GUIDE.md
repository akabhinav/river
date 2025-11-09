# River Platform - Complete Testing Guide

This guide provides step-by-step instructions for testing all 13 connectors locally using Docker Compose.

## Prerequisites

- Docker and Docker Compose installed
- At least 8GB RAM available for Docker
- Ports 3000, 3306, 5432, 6379, 8080, 9091, 9092, 9200, 27017 available

## Quick Start

### 1. Build the River Platform

```bash
cd /home/user/river

# Build the entire project
mvn clean package -DskipTests

# Verify build artifacts
ls -lh river-api/target/river-api-*.jar
ls -lh river-connectors-source/target/river-connectors-source-*.jar
ls -lh river-connectors-dest/target/river-connectors-dest-*.jar
```

### 2. Start All Services

```bash
# Start entire stack (10 services)
docker-compose up -d

# Wait for services to initialize (30-60 seconds)
echo "Waiting for services to start..."
sleep 60

# Verify all services are healthy
docker-compose ps
```

Expected output: All services should show "Up" or "healthy" status.

### 3. Verify Individual Services

Run the validation script:
```bash
./scripts/verify-services.sh
```

Or manually check each service:

#### PostgreSQL
```bash
docker exec -it river-postgres psql -U river -d river_metadata -c "\dt"
```

#### MySQL
```bash
docker exec -it river-mysql mysql -uriver -priver_pass -e "SHOW DATABASES;"
```

#### MongoDB
```bash
docker exec -it river-mongodb mongosh -u river -p river_pass --eval "show dbs"
```

#### Redis
```bash
docker exec -it river-redis redis-cli PING
```

#### Kafka
```bash
docker exec -it river-kafka kafka-topics --bootstrap-server localhost:29092 --list
```

#### Elasticsearch
```bash
curl -X GET "http://localhost:9200/_cluster/health?pretty"
```

#### River API
```bash
curl -X GET "http://localhost:8080/actuator/health"
```

## Testing Strategy

We'll test all connectors in three phases:

1. **Phase 1**: Individual connector validation (read/write operations)
2. **Phase 2**: Pipeline creation and execution
3. **Phase 3**: End-to-end data flow verification

---

## Phase 1: Individual Connector Validation

### 1.1 PostgreSQL Source Connector

**Setup Test Data:**
```bash
docker exec -it river-postgres psql -U river -d river_metadata << EOF
CREATE TABLE IF NOT EXISTS test_users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO test_users (name, email) VALUES
    ('Alice', 'alice@example.com'),
    ('Bob', 'bob@example.com'),
    ('Charlie', 'charlie@example.com');

SELECT * FROM test_users;
EOF
```

**Test Read Operation:**
```bash
curl -X POST http://localhost:8080/api/v1/connectors/test \
  -H "Content-Type: application/json" \
  -d '{
    "type": "postgresql-source",
    "connection": {
      "host": "postgres",
      "port": 5432,
      "database": "river_metadata",
      "username": "river",
      "password": "river_pass",
      "query": "SELECT * FROM test_users"
    }
  }'
```

### 1.2 MySQL Source Connector

**Setup Test Data:**
```bash
docker exec -it river-mysql mysql -uriver -priver_pass << EOF
CREATE DATABASE IF NOT EXISTS river_test;
USE river_test;

CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    price DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO products (name, price) VALUES
    ('Laptop', 999.99),
    ('Mouse', 29.99),
    ('Keyboard', 79.99);

SELECT * FROM products;
EOF
```

**Test Read Operation:**
```bash
curl -X POST http://localhost:8080/api/v1/connectors/test \
  -H "Content-Type: application/json" \
  -d '{
    "type": "mysql-source",
    "connection": {
      "host": "mysql",
      "port": 3306,
      "database": "river_test",
      "username": "river",
      "password": "river_pass",
      "table": "products"
    }
  }'
```

### 1.3 MongoDB Source Connector

**Setup Test Data:**
```bash
docker exec -it river-mongodb mongosh -u river -p river_pass << EOF
use river_test;

db.customers.insertMany([
    { name: "John Doe", email: "john@example.com", status: "active" },
    { name: "Jane Smith", email: "jane@example.com", status: "active" },
    { name: "Bob Wilson", email: "bob@example.com", status: "inactive" }
]);

db.customers.find().pretty();
EOF
```

**Test Read Operation:**
```bash
curl -X POST http://localhost:8080/api/v1/connectors/test \
  -H "Content-Type: application/json" \
  -d '{
    "type": "mongodb-source",
    "connection": {
      "uri": "mongodb://river:river_pass@mongodb:27017",
      "database": "river_test",
      "collection": "customers",
      "filter": "{\"status\":\"active\"}",
      "batchSize": 1000
    }
  }'
```

### 1.4 Redis Source Connector

**Setup Test Data:**
```bash
docker exec -it river-redis redis-cli << EOF
SET user:1 "Alice"
SET user:2 "Bob"
SET user:3 "Charlie"
HSET user:1:profile name "Alice" email "alice@example.com"
HSET user:2:profile name "Bob" email "bob@example.com"
KEYS user:*
EOF
```

**Test Read Operation:**
```bash
curl -X POST http://localhost:8080/api/v1/connectors/test \
  -H "Content-Type: application/json" \
  -d '{
    "type": "redis-source",
    "connection": {
      "host": "redis",
      "port": 6379,
      "database": 0,
      "pattern": "user:*",
      "scanCount": 1000
    }
  }'
```

### 1.5 Kafka Source Connector

**Setup Test Data:**
```bash
# Create topic
docker exec -it river-kafka kafka-topics \
  --bootstrap-server localhost:29092 \
  --create \
  --topic test-events \
  --partitions 3 \
  --replication-factor 1

# Produce test messages
docker exec -it river-kafka kafka-console-producer \
  --bootstrap-server localhost:29092 \
  --topic test-events << EOF
{"event": "login", "user": "alice", "timestamp": "2025-01-01T10:00:00Z"}
{"event": "purchase", "user": "bob", "amount": 99.99, "timestamp": "2025-01-01T10:05:00Z"}
{"event": "logout", "user": "alice", "timestamp": "2025-01-01T10:10:00Z"}
EOF
```

**Test Read Operation:**
```bash
curl -X POST http://localhost:8080/api/v1/connectors/test \
  -H "Content-Type: application/json" \
  -d '{
    "type": "kafka-source",
    "connection": {
      "bootstrapServers": "kafka:29092",
      "topic": "test-events",
      "groupId": "river-test-consumer",
      "autoOffsetReset": "earliest",
      "pollTimeout": 1000
    }
  }'
```

### 1.6 Elasticsearch Source Connector

**Setup Test Data:**
```bash
# Create index and add documents
curl -X PUT "http://localhost:9200/test-logs" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "properties": {
      "level": { "type": "keyword" },
      "message": { "type": "text" },
      "timestamp": { "type": "date" }
    }
  }
}'

curl -X POST "http://localhost:9200/test-logs/_bulk" -H 'Content-Type: application/json' -d'
{"index":{}}
{"level":"INFO","message":"Application started","timestamp":"2025-01-01T10:00:00Z"}
{"index":{}}
{"level":"ERROR","message":"Connection failed","timestamp":"2025-01-01T10:05:00Z"}
{"index":{}}
{"level":"WARN","message":"High memory usage","timestamp":"2025-01-01T10:10:00Z"}
'

# Verify data
curl -X GET "http://localhost:9200/test-logs/_search?pretty"
```

**Test Read Operation:**
```bash
curl -X POST http://localhost:8080/api/v1/connectors/test \
  -H "Content-Type: application/json" \
  -d '{
    "type": "elasticsearch-source",
    "connection": {
      "host": "elasticsearch",
      "port": 9200,
      "index": "test-logs",
      "query": "{\"query\":{\"match_all\":{}}}",
      "scrollSize": 1000,
      "scrollTimeout": "1m"
    }
  }'
```

### 1.7 S3 Source Connector

**Setup Test Data (MinIO as S3 alternative for local testing):**

*Note: You can add MinIO to docker-compose.yml for local S3 testing, or test with real AWS S3 credentials.*

For local testing with MinIO:
```bash
# Add MinIO service to docker-compose.yml
# Then create bucket and upload test files
```

---

## Phase 2: Pipeline Creation and Execution

### 2.1 PostgreSQL → Redis (Caching)

```bash
# Create pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/postgres-to-redis-cache-pipeline.json

# Check pipeline status
curl -X GET http://localhost:8080/api/v1/pipelines/postgres-to-redis-cache

# Start pipeline execution
curl -X POST http://localhost:8080/api/v1/pipelines/postgres-to-redis-cache/start

# Verify data in Redis
docker exec -it river-redis redis-cli KEYS "*"
docker exec -it river-redis redis-cli GET <key>
```

### 2.2 MongoDB → Elasticsearch (Search Indexing)

```bash
# Create pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/mongodb-to-elasticsearch-pipeline.json

# Start pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/mongodb-to-elasticsearch-sync/start

# Verify data in Elasticsearch
curl -X GET "http://localhost:9200/customers/_search?pretty"
```

### 2.3 MySQL → Kafka (CDC Streaming)

```bash
# Create pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/mysql-to-kafka-pipeline.json

# Start pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/mysql-to-kafka-cdc/start

# Consume from Kafka to verify
docker exec -it river-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic mysql-changes \
  --from-beginning
```

### 2.4 Kafka → MongoDB (Event Storage)

```bash
# Create pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/kafka-to-mongodb-pipeline.json

# Start pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/kafka-to-mongodb-events/start

# Verify events in MongoDB
docker exec -it river-mongodb mongosh -u river -p river_pass --eval "
  use river_test;
  db.events.find().pretty();
"
```

### 2.5 Elasticsearch → MySQL (Reporting)

```bash
# Create pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/elasticsearch-to-mysql-pipeline.json

# Start pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/elasticsearch-to-mysql-reporting/start

# Verify data in MySQL
docker exec -it river-mysql mysql -uriver -priver_pass -e "
  USE river_test;
  SELECT * FROM log_summary;
"
```

---

## Phase 3: End-to-End Data Flow Verification

### Test 1: Full Data Replication Flow

**PostgreSQL → MongoDB → Elasticsearch → MySQL**

This tests a complete data journey through multiple connectors.

```bash
# 1. Insert data into PostgreSQL
docker exec -it river-postgres psql -U river -d river_metadata -c "
  INSERT INTO test_users (name, email) VALUES ('Test User', 'test@example.com');
"

# 2. Create PostgreSQL → MongoDB pipeline
curl -X POST http://localhost:8080/api/v1/pipelines -H "Content-Type: application/json" -d '{
  "name": "postgres-to-mongodb",
  "mode": "BATCH",
  "source": {
    "type": "postgresql-source",
    "connection": {
      "host": "postgres",
      "port": 5432,
      "database": "river_metadata",
      "username": "river",
      "password": "river_pass",
      "query": "SELECT * FROM test_users"
    }
  },
  "destination": {
    "type": "mongodb-destination",
    "connection": {
      "uri": "mongodb://river:river_pass@mongodb:27017",
      "database": "river_test",
      "collection": "users_from_postgres"
    }
  }
}'

# 3. Start pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/postgres-to-mongodb/start

# 4. Verify in MongoDB
docker exec -it river-mongodb mongosh -u river -p river_pass --eval "
  use river_test;
  db.users_from_postgres.find().pretty();
"

# 5. Create MongoDB → Elasticsearch pipeline
curl -X POST http://localhost:8080/api/v1/pipelines -H "Content-Type: application/json" -d '{
  "name": "mongodb-to-elasticsearch",
  "mode": "BATCH",
  "source": {
    "type": "mongodb-source",
    "connection": {
      "uri": "mongodb://river:river_pass@mongodb:27017",
      "database": "river_test",
      "collection": "users_from_postgres"
    }
  },
  "destination": {
    "type": "elasticsearch-destination",
    "connection": {
      "host": "elasticsearch",
      "port": 9200,
      "index": "users"
    }
  }
}'

# 6. Start pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/mongodb-to-elasticsearch/start

# 7. Verify in Elasticsearch
curl -X GET "http://localhost:9200/users/_search?pretty"
```

### Test 2: Streaming Data Flow

**MySQL → Kafka → MongoDB (Real-time)**

```bash
# 1. Start streaming pipelines
curl -X POST http://localhost:8080/api/v1/pipelines/mysql-to-kafka-cdc/start
curl -X POST http://localhost:8080/api/v1/pipelines/kafka-to-mongodb-events/start

# 2. Insert data into MySQL
docker exec -it river-mysql mysql -uriver -priver_pass -e "
  USE river_test;
  INSERT INTO products (name, price) VALUES ('New Product', 199.99);
"

# 3. Wait a few seconds for streaming
sleep 5

# 4. Verify in Kafka
docker exec -it river-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic mysql-changes \
  --from-beginning \
  --max-messages 1

# 5. Verify in MongoDB
docker exec -it river-mongodb mongosh -u river -p river_pass --eval "
  use river_test;
  db.events.find().sort({_id:-1}).limit(1).pretty();
"
```

### Test 3: Caching Pattern

**PostgreSQL → Redis → Validate TTL**

```bash
# 1. Start caching pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/postgres-to-redis-cache/start

# 2. Check Redis for cached data
docker exec -it river-redis redis-cli KEYS "*"

# 3. Get a cached value
docker exec -it river-redis redis-cli GET <key>

# 4. Check TTL
docker exec -it river-redis redis-cli TTL <key>

# 5. Update PostgreSQL data
docker exec -it river-postgres psql -U river -d river_metadata -c "
  UPDATE test_users SET email = 'newemail@example.com' WHERE id = 1;
"

# 6. Manually trigger cache refresh
curl -X POST http://localhost:8080/api/v1/pipelines/postgres-to-redis-cache/start

# 7. Verify updated value in Redis
docker exec -it river-redis redis-cli GET <key>
```

---

## Monitoring and Metrics

### View Prometheus Metrics

```bash
# Access Prometheus UI
open http://localhost:9091

# Query pipeline metrics
curl http://localhost:9091/api/v1/query?query=river_pipeline_records_processed_total

# Query connector metrics
curl http://localhost:9091/api/v1/query?query=river_connector_read_duration_seconds
```

### View Grafana Dashboards

```bash
# Access Grafana
open http://localhost:3000

# Login: admin / admin

# Import River Platform dashboard (if created)
```

### View API Logs

```bash
# River API logs
docker logs -f river-api

# Filter for specific pipeline
docker logs river-api 2>&1 | grep "pipeline-name"
```

---

## Troubleshooting

### Services Not Starting

```bash
# Check logs for specific service
docker logs river-postgres
docker logs river-mysql
docker logs river-mongodb
docker logs river-redis
docker logs river-kafka
docker logs river-elasticsearch

# Check resource usage
docker stats

# Restart specific service
docker-compose restart <service-name>
```

### Pipeline Execution Failures

```bash
# Check pipeline status
curl -X GET http://localhost:8080/api/v1/pipelines/<pipeline-name>

# Check execution logs
curl -X GET http://localhost:8080/api/v1/pipelines/<pipeline-name>/executions

# View detailed error
docker logs river-api | grep ERROR
```

### Connection Issues

```bash
# Test connectivity between services
docker exec -it river-api ping postgres
docker exec -it river-api ping mysql
docker exec -it river-api ping mongodb
docker exec -it river-api ping redis
docker exec -it river-api ping kafka
docker exec -it river-api ping elasticsearch

# Check network
docker network ls
docker network inspect river_river-network
```

### Data Not Flowing

```bash
# Verify source connector can read
curl -X POST http://localhost:8080/api/v1/connectors/test -d @source-config.json

# Verify destination connector can write
curl -X POST http://localhost:8080/api/v1/connectors/test -d @dest-config.json

# Check pipeline configuration
curl -X GET http://localhost:8080/api/v1/pipelines/<pipeline-name>

# Manually trigger execution
curl -X POST http://localhost:8080/api/v1/pipelines/<pipeline-name>/start
```

---

## Performance Testing

### Load Testing Single Connector

```bash
# Insert large dataset into PostgreSQL
docker exec -it river-postgres psql -U river -d river_metadata << EOF
INSERT INTO test_users (name, email)
SELECT
  'User ' || generate_series,
  'user' || generate_series || '@example.com'
FROM generate_series(1, 100000);
EOF

# Time the data transfer
time curl -X POST http://localhost:8080/api/v1/pipelines/postgres-to-redis-cache/start

# Check metrics
curl http://localhost:9091/api/v1/query?query=river_pipeline_execution_duration_seconds
```

### Streaming Performance

```bash
# Produce 10,000 messages to Kafka
docker exec -it river-kafka kafka-producer-perf-test \
  --topic test-events \
  --num-records 10000 \
  --record-size 100 \
  --throughput 1000 \
  --producer-props bootstrap.servers=localhost:29092

# Monitor consumer lag
docker exec -it river-kafka kafka-consumer-groups \
  --bootstrap-server localhost:29092 \
  --describe \
  --group river-consumer
```

---

## Cleanup

### Stop All Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v

# Remove all images
docker-compose down --rmi all
```

### Reset Individual Service

```bash
# Reset PostgreSQL
docker-compose rm -sf postgres
docker volume rm river_postgres-data
docker-compose up -d postgres

# Reset MongoDB
docker-compose rm -sf mongodb
docker volume rm river_mongodb-data
docker-compose up -d mongodb
```

---

## Test Checklist

Use this checklist to track your testing progress:

### Individual Connectors
- [ ] PostgreSQL Source - Read test data
- [ ] PostgreSQL Destination - Write test data
- [ ] MySQL Source - Read test data
- [ ] MySQL Destination - Write test data
- [ ] MongoDB Source - Read test data
- [ ] MongoDB Destination - Write test data
- [ ] Redis Source - Read test data
- [ ] Redis Destination - Write test data with TTL
- [ ] Kafka Source - Consume messages
- [ ] Kafka Destination - Produce messages
- [ ] Elasticsearch Source - Query and scroll
- [ ] Elasticsearch Destination - Bulk index
- [ ] S3 Source - Read files (if configured)

### Example Pipelines
- [ ] PostgreSQL → Redis (Caching)
- [ ] MongoDB → Elasticsearch (Search)
- [ ] MySQL → Kafka (CDC)
- [ ] Kafka → MongoDB (Events)
- [ ] Elasticsearch → MySQL (Reporting)

### Advanced Scenarios
- [ ] Multi-hop pipeline (4+ connectors)
- [ ] Real-time streaming (Kafka-based)
- [ ] Large dataset transfer (100K+ records)
- [ ] Error handling and retry
- [ ] Connection failure recovery

### Monitoring
- [ ] Prometheus metrics visible
- [ ] Grafana dashboards working
- [ ] API health endpoint responding
- [ ] Logs accessible for all services

---

## Next Steps

After completing these tests:

1. Review the [Connector Development Guide](docs/connector-development.md) to build custom connectors
2. Check [Architecture Documentation](docs/architecture.md) for platform internals
3. Explore advanced features like transformations and custom processors
4. Set up production deployment using Kubernetes configs in `/kubernetes`

---

## Support

If you encounter issues:

1. Check the troubleshooting section above
2. Review service logs: `docker logs <service-name>`
3. Verify configurations in `docker-compose.yml`
4. Check example pipelines in `/examples` directory
5. Review connector documentation in `docs/connectors-list.md`

Happy Testing! 🚀
