# River Platform Quick Start Guide

Get started with River Platform in 5 minutes!

## Prerequisites

- Java 21 or later
- Maven 3.8+
- Docker and Docker Compose (for full stack)

## Quick Start Options

### Option 1: Docker Compose (Recommended)

Run the entire stack with one command:

```bash
# Clone the repository
git clone https://github.com/your-org/river.git
cd river

# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f river
```

Services available:
- River API: http://localhost:8080
- Prometheus: http://localhost:9091
- Grafana: http://localhost:3000 (admin/admin)
- PostgreSQL: localhost:5432

### Option 2: Build and Run Locally

```bash
# Build the project
mvn clean install

# Run the application
java -jar river-runtime/target/river-runtime-1.0.0-SNAPSHOT.jar
```

### Option 3: Kubernetes

```bash
# Build Docker image
docker build -t river-platform:latest .

# Deploy to Kubernetes
kubectl apply -f deployment/kubernetes/

# Check deployment
kubectl get pods
kubectl get services
```

## Your First Pipeline

### Step 1: Create a Pipeline Definition

Create a file `my-pipeline.json`:

```json
{
  "name": "postgres-to-s3",
  "description": "Extract data from PostgreSQL and load to S3",
  "mode": "BATCH",
  "source": {
    "type": "postgresql-source",
    "connection": {
      "host": "localhost",
      "port": 5432,
      "database": "mydb",
      "username": "user",
      "password": "pass",
      "table": "users"
    },
    "readMode": "FULL",
    "parallelism": 4
  },
  "destination": {
    "type": "s3-destination",
    "connection": {
      "bucket": "my-bucket",
      "prefix": "data/users/",
      "region": "us-east-1",
      "accessKey": "ACCESS_KEY",
      "secretKey": "SECRET_KEY"
    },
    "writeMode": "APPEND",
    "batchSize": 1000
  },
  "schedule": "0 0 * * *"
}
```

### Step 2: Create the Pipeline via API

```bash
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @my-pipeline.json
```

Response:
```json
{
  "id": "pipeline-123",
  "name": "postgres-to-s3",
  "state": "CREATED",
  "createdAt": "2024-01-15T10:00:00Z"
}
```

### Step 3: Start the Pipeline

```bash
curl -X POST http://localhost:8080/api/v1/pipelines/pipeline-123/start
```

### Step 4: Monitor the Pipeline

```bash
# Get pipeline status
curl http://localhost:8080/api/v1/pipelines/pipeline-123

# View metrics
curl http://localhost:8080/actuator/metrics
```

## Common Use Cases

### Use Case 1: Database Replication

```json
{
  "name": "mysql-to-postgres",
  "source": {
    "type": "mysql-source",
    "connection": {
      "host": "mysql.example.com",
      "database": "orders",
      "table": "transactions"
    },
    "readMode": "INCREMENTAL",
    "incrementalColumn": "updated_at"
  },
  "destination": {
    "type": "postgresql-destination",
    "connection": {
      "host": "postgres.example.com",
      "database": "analytics",
      "table": "transactions"
    },
    "writeMode": "UPSERT",
    "upsertKeys": ["id"]
  }
}
```

### Use Case 2: Real-time Streaming

```json
{
  "name": "kafka-to-mongodb",
  "mode": "STREAMING",
  "source": {
    "type": "kafka-source",
    "connection": {
      "bootstrapServers": "kafka:9092",
      "topic": "events",
      "groupId": "river-consumer"
    }
  },
  "destination": {
    "type": "mongodb-destination",
    "connection": {
      "uri": "mongodb://localhost:27017",
      "database": "events",
      "collection": "user_events"
    }
  }
}
```

### Use Case 3: Data Lake Ingestion

```json
{
  "name": "api-to-delta-lake",
  "source": {
    "type": "rest-api-source",
    "connection": {
      "endpoint": "https://api.example.com/data",
      "apiKey": "YOUR_API_KEY"
    }
  },
  "destination": {
    "type": "delta-lake-destination",
    "connection": {
      "path": "s3://my-bucket/delta-lake/data",
      "format": "delta"
    }
  }
}
```

## API Reference

### Pipelines

- `POST /api/v1/pipelines` - Create pipeline
- `GET /api/v1/pipelines` - List pipelines
- `GET /api/v1/pipelines/{id}` - Get pipeline
- `POST /api/v1/pipelines/{id}/start` - Start pipeline
- `POST /api/v1/pipelines/{id}/stop` - Stop pipeline
- `DELETE /api/v1/pipelines/{id}` - Delete pipeline

### Health & Metrics

- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

## Configuration

Edit `application.yml`:

```yaml
river:
  mode: standalone  # or distributed
  connectors:
    scan-packages:
      - com.river.connector.source
      - com.river.connector.dest

logging:
  level:
    com.river: DEBUG
```

## Troubleshooting

### Pipeline Not Starting

Check logs:
```bash
docker-compose logs river
```

### Connection Errors

Verify connectivity:
```bash
# Test PostgreSQL connection
docker exec -it river_postgres_1 psql -U river -d river_metadata

# Test network
docker-compose exec river ping postgres
```

### Performance Issues

Check metrics in Prometheus/Grafana:
- Throughput: `river_records_processed_total`
- Latency: `river_pipeline_duration_seconds`
- Errors: `river_errors_total`

## Next Steps

- [Architecture Guide](architecture.md) - Understand the platform design
- [Connector Development](connector-development.md) - Build custom connectors
- [Deployment Guide](deployment.md) - Production deployment
- [API Documentation](api.md) - Full API reference

## Getting Help

- Documentation: https://docs.river-platform.io
- GitHub Issues: https://github.com/your-org/river/issues
- Community: https://community.river-platform.io
