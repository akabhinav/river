# Quick Test Guide - River Platform

## 🚀 Fastest Way to Test (2 Minutes)

```bash
# 1. Run automated test script
./scripts/test-local.sh full

# That's it! The script will:
# ✓ Check prerequisites
# ✓ Build the project
# ✓ Run unit tests
# ✓ Start Docker services
# ✓ Test all components
# ✓ Show you all access URLs
```

## 📋 What Gets Tested

| Component | What's Tested | Status |
|-----------|---------------|--------|
| **Maven Build** | Compilation, dependencies | ✓ Full |
| **Unit Tests** | Core, Engine, Connectors | ✓ Full |
| **Docker** | All services startup | ✓ Full |
| **API** | Health, endpoints, CRUD | ✓ Full |
| **PostgreSQL** | Connection, queries | ✓ Full |
| **Redis** | Connection, ping | ✓ Full |
| **Metrics** | Prometheus metrics | ✓ Full |
| **Pipelines** | Create, retrieve, delete | ✓ Full |

## 🎯 Testing Options

### Option 1: Full Automated Test
```bash
./scripts/test-local.sh full
```
Tests everything automatically (recommended for first time)

### Option 2: Manual Docker Compose
```bash
# Start services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f river

# Stop services
docker-compose down
```

### Option 3: Individual Components

#### Test Prerequisites Only
```bash
./scripts/test-local.sh prereq
```

#### Build Only
```bash
./scripts/test-local.sh build
```

#### Unit Tests Only
```bash
./scripts/test-local.sh test
```

#### Docker Services Only
```bash
./scripts/test-local.sh docker
```

## 🧪 Advanced Testing

### Test Pipeline Execution
```bash
# Start services first
docker-compose up -d

# Create test data and run E2E test
./scripts/test-pipeline-execution.sh
```

### Create Sample Test Data
```bash
# Start PostgreSQL first
docker-compose up -d postgres

# Create test data
./scripts/create-test-data.sh
```

### Manual API Testing
```bash
# Health check
curl http://localhost:8080/actuator/health

# Create pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/postgres-to-postgres-pipeline.json

# List pipelines
curl http://localhost:8080/api/v1/pipelines | jq '.'

# Get metrics
curl http://localhost:8080/actuator/prometheus
```

## 🌐 Access URLs (After Starting)

| Service | URL | Credentials |
|---------|-----|-------------|
| **River API** | http://localhost:8080 | - |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | - |
| **Health Check** | http://localhost:8080/actuator/health | - |
| **Metrics** | http://localhost:8080/actuator/prometheus | - |
| **Prometheus** | http://localhost:9091 | - |
| **Grafana** | http://localhost:3000 | admin/admin |
| **PostgreSQL** | localhost:5432 | river/river_pass |
| **Redis** | localhost:6379 | - |

## 🔍 Quick Verification Commands

```bash
# Check all services are running
docker-compose ps

# API health
curl http://localhost:8080/actuator/health

# PostgreSQL connection
docker exec -it river-postgres-1 psql -U river -d river_metadata

# Redis connection
docker exec -it river-redis-1 redis-cli ping

# View logs
docker-compose logs -f river

# Check metrics
curl http://localhost:8080/actuator/prometheus | grep jvm_memory_used_bytes
```

## 🐛 Troubleshooting

### Services won't start
```bash
# Check ports are free
lsof -i :8080
lsof -i :5432

# Remove old containers and volumes
docker-compose down -v

# Start fresh
docker-compose up -d
```

### Build fails
```bash
# Clean Maven cache
rm -rf ~/.m2/repository/com/river

# Rebuild
mvn clean install -U
```

### API not responding
```bash
# Check logs
docker-compose logs river

# Restart service
docker-compose restart river
```

### Database connection issues
```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Check logs
docker-compose logs postgres

# Try connecting manually
docker exec -it river-postgres-1 psql -U river -d river_metadata
```

## 🧹 Cleanup

```bash
# Stop all services
docker-compose down

# Remove volumes (deletes all data)
docker-compose down -v

# Clean Maven build
mvn clean

# Complete cleanup
./scripts/test-local.sh cleanup
```

## ✅ Success Checklist

After running tests, you should see:

- [x] Maven build successful
- [x] All unit tests passing
- [x] Docker services running
- [x] API health check returns `{"status":"UP"}`
- [x] Can create pipelines via API
- [x] Can retrieve pipelines
- [x] PostgreSQL accessible
- [x] Redis responding to ping
- [x] Metrics available at `/actuator/prometheus`
- [x] Prometheus scraping metrics
- [x] Grafana accessible

## 📊 Sample Test Data

After running `create-test-data.sh`, you'll have:

- **test_source.users** - 110 sample users
- **test_source.orders** - 500 sample orders
- **test_destination.users** - Empty, ready for pipeline testing
- **test_destination.orders** - Empty, ready for pipeline testing

## 🎓 What to Test Next

1. **Create your first real pipeline**
   ```bash
   curl -X POST http://localhost:8080/api/v1/pipelines \
     -H "Content-Type: application/json" \
     -d @examples/postgres-to-postgres-pipeline.json
   ```

2. **Monitor with Prometheus**
   - Open http://localhost:9091
   - Run query: `rate(http_server_requests_seconds_count[1m])`

3. **Visualize in Grafana**
   - Open http://localhost:3000
   - Login: admin/admin
   - Add Prometheus data source: http://prometheus:9090

4. **Test with real data**
   - Connect your own PostgreSQL database
   - Try S3 connector with LocalStack
   - Implement additional connectors

## 📚 Documentation

- [Full Local Testing Guide](docs/local-testing-guide.md)
- [Architecture Guide](docs/architecture.md)
- [Connector Development](docs/connector-development.md)
- [Quick Start](docs/quickstart.md)

## 🆘 Getting Help

If you encounter issues:

1. Check logs: `docker-compose logs`
2. Review [Local Testing Guide](docs/local-testing-guide.md)
3. Ensure prerequisites are installed correctly
4. Try cleanup and restart: `docker-compose down -v && docker-compose up -d`

---

**Ready to test?** Run: `./scripts/test-local.sh full`
