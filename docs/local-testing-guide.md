# Local Testing Guide

This guide walks you through testing all River Platform components on your local machine.

## Prerequisites

Install these tools before starting:

```bash
# Check Java version (must be 21+)
java -version

# Check Maven version (must be 3.8+)
mvn -version

# Check Docker and Docker Compose
docker --version
docker-compose --version
```

## Testing Options

### Option 1: Quick Test with Docker Compose (Recommended)
- Tests: Full stack, all integrations, connectors, API
- Time: 5 minutes
- Complexity: Easy

### Option 2: Maven Build and Unit Tests
- Tests: Core logic, connectors, pipeline engine
- Time: 2 minutes
- Complexity: Easy

### Option 3: Run Application Standalone
- Tests: API, single connectors, runtime
- Time: 10 minutes
- Complexity: Medium

---

## Option 1: Full Stack Testing with Docker Compose

### Step 1: Start the Full Stack

```bash
# Navigate to project directory
cd /home/user/river

# Start all services
docker-compose up -d

# Check all services are running
docker-compose ps
```

Expected output:
```
NAME                COMMAND                  SERVICE             STATUS
river-grafana-1     "/run.sh"                grafana             Up
river-postgres-1    "docker-entrypoint..."   postgres            Up
river-prometheus-1  "/bin/prometheus..."     prometheus          Up
river-redis-1       "docker-entrypoint..."   redis               Up
river-river-1       "java -jar app.jar"      river               Up
```

### Step 2: Verify Each Service

#### A. River API (Port 8080)

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}

# List pipelines (should be empty initially)
curl http://localhost:8080/api/v1/pipelines

# Expected response:
# []
```

#### B. PostgreSQL (Port 5432)

```bash
# Connect to PostgreSQL
docker exec -it river-postgres-1 psql -U river -d river_metadata

# Inside psql, run:
\dt          # List tables
\q           # Quit
```

#### C. Redis (Port 6379)

```bash
# Test Redis
docker exec -it river-redis-1 redis-cli ping

# Expected response:
# PONG
```

#### D. Prometheus (Port 9091)

```bash
# Check Prometheus is scraping metrics
curl http://localhost:9091/targets

# Or open in browser:
# http://localhost:9091
```

#### E. Grafana (Port 3000)

```bash
# Open in browser:
# http://localhost:3000
# Login: admin / admin
```

### Step 3: Test Pipeline Creation

#### A. Create a Simple Pipeline

```bash
# Create pipeline configuration
cat > /tmp/test-pipeline.json <<'EOF'
{
  "name": "test-pipeline",
  "description": "Test pipeline for local verification",
  "mode": "BATCH",
  "source": {
    "type": "postgresql-source",
    "connection": {
      "host": "postgres",
      "port": 5432,
      "database": "river_metadata",
      "username": "river",
      "password": "river_pass",
      "table": "pg_catalog.pg_tables"
    },
    "readMode": "FULL"
  },
  "destination": {
    "type": "postgresql-destination",
    "connection": {
      "host": "postgres",
      "port": 5432,
      "database": "river_metadata",
      "username": "river",
      "password": "river_pass",
      "table": "test_output"
    },
    "writeMode": "APPEND",
    "batchSize": 100
  }
}
EOF

# Create the pipeline via API
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @/tmp/test-pipeline.json

# Expected response: Pipeline created with ID
```

#### B. List Pipelines

```bash
# Get all pipelines
curl http://localhost:8080/api/v1/pipelines | jq '.'

# Get specific pipeline (replace {id} with actual ID)
curl http://localhost:8080/api/v1/pipelines/{id} | jq '.'
```

### Step 4: View Logs

```bash
# View River application logs
docker-compose logs -f river

# View specific service logs
docker-compose logs postgres
docker-compose logs redis
docker-compose logs prometheus
```

### Step 5: Test Metrics and Monitoring

```bash
# Get application metrics
curl http://localhost:8080/actuator/metrics

# Get Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# View in Prometheus UI
# http://localhost:9091/graph
# Query examples:
# - jvm_memory_used_bytes
# - process_cpu_usage
# - http_server_requests_seconds_count
```

### Step 6: Cleanup

```bash
# Stop all services
docker-compose down

# Remove volumes (caution: deletes all data)
docker-compose down -v
```

---

## Option 2: Maven Build and Unit Tests

### Step 1: Build the Project

```bash
cd /home/user/river

# Clean and build
mvn clean install

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Total time: ~2-3 minutes
```

### Step 2: Run Unit Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl river-core
mvn test -pl river-engine
mvn test -pl river-connectors-source

# Run specific test class
mvn test -pl river-core -Dtest=RecordTest

# Skip tests during build
mvn clean install -DskipTests
```

### Step 3: Verify Build Artifacts

```bash
# Check built JARs
find . -name "*.jar" -type f

# Expected files:
# ./river-core/target/river-core-1.0.0-SNAPSHOT.jar
# ./river-engine/target/river-engine-1.0.0-SNAPSHOT.jar
# ./river-runtime/target/river-runtime-1.0.0-SNAPSHOT.jar
```

### Step 4: Code Coverage (Optional)

```bash
# Generate coverage report
mvn clean test jacoco:report

# View coverage reports
# Open: river-core/target/site/jacoco/index.html
```

---

## Option 3: Run Application Standalone

### Step 1: Start Required Services

You need PostgreSQL and Redis running. Use Docker for these:

```bash
# Start PostgreSQL
docker run -d \
  --name river-postgres \
  -e POSTGRES_DB=river_metadata \
  -e POSTGRES_USER=river \
  -e POSTGRES_PASSWORD=river_pass \
  -p 5432:5432 \
  postgres:16-alpine

# Start Redis
docker run -d \
  --name river-redis \
  -p 6379:6379 \
  redis:7-alpine

# Verify they're running
docker ps
```

### Step 2: Build the Application

```bash
cd /home/user/river
mvn clean package -DskipTests
```

### Step 3: Run River Application

```bash
# Run the application
java -jar river-runtime/target/river-runtime-1.0.0-SNAPSHOT.jar

# Or with specific profile
java -jar river-runtime/target/river-runtime-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=local

# With custom port
java -jar river-runtime/target/river-runtime-1.0.0-SNAPSHOT.jar \
  --server.port=9090
```

### Step 4: Verify Application Started

```bash
# In another terminal, check health
curl http://localhost:8080/actuator/health

# Check info
curl http://localhost:8080/actuator/info

# List pipelines
curl http://localhost:8080/api/v1/pipelines
```

### Step 5: Test API Endpoints

```bash
# Get API documentation (Swagger UI)
# Open in browser: http://localhost:8080/swagger-ui.html

# Test health endpoint
curl http://localhost:8080/actuator/health

# Test metrics endpoint
curl http://localhost:8080/actuator/metrics

# Create a simple pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d '{
    "name": "standalone-test",
    "description": "Test pipeline",
    "mode": "BATCH",
    "source": {
      "type": "postgresql-source",
      "connection": {
        "host": "localhost",
        "port": 5432,
        "database": "river_metadata",
        "username": "river",
        "password": "river_pass"
      }
    },
    "destination": {
      "type": "postgresql-destination",
      "connection": {
        "host": "localhost",
        "port": 5432,
        "database": "river_metadata",
        "username": "river",
        "password": "river_pass"
      }
    }
  }'
```

### Step 6: Stop Services

```bash
# Stop Java application: Ctrl+C

# Stop Docker containers
docker stop river-postgres river-redis
docker rm river-postgres river-redis
```

---

## Component-by-Component Testing

### 1. Core SDK (river-core)

```bash
# Test connector abstractions
cd river-core

# Run tests
mvn test

# Test specific functionality
mvn test -Dtest=ConnectorRegistryTest
mvn test -Dtest=RecordTest
mvn test -Dtest=SchemaTest
```

### 2. Pipeline Engine (river-engine)

```bash
cd river-engine

# Run pipeline engine tests
mvn test

# Test pipeline execution
mvn test -Dtest=PipelineExecutorTest
```

### 3. Connectors

#### PostgreSQL Connector (requires PostgreSQL running)

```bash
# Start PostgreSQL
docker run -d --name test-postgres \
  -e POSTGRES_PASSWORD=test \
  -p 5432:5432 \
  postgres:16-alpine

# Create test data
docker exec -it test-postgres psql -U postgres -c "
  CREATE TABLE test_users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
  );
  INSERT INTO test_users (name, email) VALUES
    ('Alice', 'alice@example.com'),
    ('Bob', 'bob@example.com'),
    ('Charlie', 'charlie@example.com');
"

# Run connector tests
cd river-connectors-source
mvn test -Dtest=PostgresSourceConnectorTest

# Cleanup
docker stop test-postgres
docker rm test-postgres
```

#### S3 Connector (requires AWS credentials or LocalStack)

```bash
# Option A: Use LocalStack for local S3 testing
docker run -d --name localstack \
  -p 4566:4566 \
  -e SERVICES=s3 \
  localstack/localstack

# Create test bucket
aws --endpoint-url=http://localhost:4566 s3 mb s3://test-bucket

# Run S3 connector tests
cd river-connectors-source
mvn test -Dtest=S3SourceConnectorTest
```

### 4. REST API

```bash
# Run API tests
cd river-api
mvn test

# Test controller
mvn test -Dtest=PipelineControllerTest

# Test service layer
mvn test -Dtest=PipelineServiceTest
```

---

## Integration Testing Scenarios

### Scenario 1: PostgreSQL to PostgreSQL Replication

```bash
# 1. Start Docker services
docker-compose up -d postgres

# 2. Create source table and data
docker exec -it river-postgres-1 psql -U river -d river_metadata -c "
  CREATE TABLE source_table (
    id SERIAL PRIMARY KEY,
    data VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
  );
  INSERT INTO source_table (data)
  SELECT 'Record ' || generate_series(1, 1000);
"

# 3. Create destination table
docker exec -it river-postgres-1 psql -U river -d river_metadata -c "
  CREATE TABLE dest_table (
    id INTEGER,
    data VARCHAR(100),
    created_at TIMESTAMP
  );
"

# 4. Create pipeline using the example
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/postgres-to-postgres-pipeline.json

# 5. Start the pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/{pipeline-id}/start

# 6. Verify data copied
docker exec -it river-postgres-1 psql -U river -d river_metadata -c "
  SELECT COUNT(*) FROM dest_table;
"
```

### Scenario 2: Test Metrics Collection

```bash
# 1. Start full stack
docker-compose up -d

# 2. Generate some load by creating pipelines
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/v1/pipelines \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"test-pipeline-$i\", \"mode\":\"BATCH\"}"
done

# 3. Check Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep river

# 4. View in Prometheus UI
# http://localhost:9091
# Query: rate(http_server_requests_seconds_count[1m])
```

---

## Troubleshooting

### Issue: Port Already in Use

```bash
# Check what's using the port
lsof -i :8080
netstat -tuln | grep 8080

# Kill the process
kill -9 <PID>

# Or use different port
java -jar river-runtime/target/river-runtime-1.0.0-SNAPSHOT.jar \
  --server.port=9090
```

### Issue: Maven Build Fails

```bash
# Clear Maven cache
rm -rf ~/.m2/repository/com/river

# Rebuild
mvn clean install -U

# Skip tests if needed
mvn clean install -DskipTests
```

### Issue: Docker Compose Fails

```bash
# View logs
docker-compose logs

# Rebuild images
docker-compose build --no-cache

# Remove all containers and start fresh
docker-compose down -v
docker-compose up -d
```

### Issue: Cannot Connect to Database

```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Test connection
docker exec -it river-postgres-1 psql -U river -d river_metadata

# Check connection from host
psql -h localhost -p 5432 -U river -d river_metadata
```

### Issue: Application Won't Start

```bash
# Check Java version
java -version  # Must be 21+

# Check for errors in logs
docker-compose logs river

# Run with debug logging
java -jar river-runtime/target/river-runtime-1.0.0-SNAPSHOT.jar \
  --logging.level.com.river=DEBUG
```

---

## What You Can Test Locally

✅ **Fully Testable**
- Maven build and compilation
- Unit tests for all modules
- Core SDK functionality
- Pipeline engine orchestration
- REST API endpoints
- Docker containerization
- PostgreSQL connectors (with Docker PostgreSQL)
- Metrics and monitoring
- Health checks
- API documentation (Swagger)

⚠️ **Partially Testable** (requires additional setup)
- S3 connectors (use LocalStack or real AWS account)
- Kafka connectors (need Kafka cluster)
- Distributed mode (need multiple instances)
- Production-scale performance testing

❌ **Not Testable Locally** (require cloud/production environment)
- Kubernetes deployment
- Multi-region deployment
- Large-scale data processing (100GB+)
- Real production workloads

---

## Quick Verification Checklist

Run this checklist to verify everything works:

```bash
# 1. Build succeeds
mvn clean install
echo "✓ Build successful"

# 2. Docker Compose starts
docker-compose up -d
sleep 10
echo "✓ Docker services started"

# 3. API responds
curl -f http://localhost:8080/actuator/health && echo "✓ API healthy"

# 4. Can create pipeline
curl -f -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d '{"name":"test"}' && echo "✓ Pipeline creation works"

# 5. PostgreSQL accessible
docker exec river-postgres-1 psql -U river -d river_metadata -c "SELECT 1" && echo "✓ PostgreSQL works"

# 6. Metrics available
curl -f http://localhost:8080/actuator/prometheus > /dev/null && echo "✓ Metrics working"

# Cleanup
docker-compose down

echo ""
echo "All checks passed! ✓"
```

---

## Next Steps

After local testing, you can:

1. Add more connectors (MySQL, MongoDB, Kafka)
2. Implement transformation layer
3. Add integration tests
4. Test with real data sources
5. Deploy to Kubernetes for distributed testing
6. Set up CI/CD pipeline
