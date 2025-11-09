# River Platform - Quick Test Reference

One-page reference for testing River Platform locally.

## 🚀 Quick Start (5 minutes)

```bash
# 1. Start all services
docker-compose up -d

# 2. Wait for initialization (60 seconds)
sleep 60

# 3. Run complete test suite
./scripts/run-all-tests.sh
```

That's it! The script will automatically:
- ✅ Verify all services
- ✅ Setup test data
- ✅ Test all 13 connectors
- ✅ Run example pipelines
- ✅ Generate test report

---

## 📋 Individual Test Scripts

### 1. Verify Services
```bash
./scripts/verify-services.sh
```
Checks that all 9 services are running and healthy.

### 2. Setup Test Data
```bash
./scripts/setup-test-data.sh
```
Creates sample data in all data stores (PostgreSQL, MySQL, MongoDB, Redis, Kafka, Elasticsearch).

### 3. Test Connectors
```bash
./scripts/test-connectors.sh
```
Tests all 13 source and destination connectors individually.

### 4. Test Pipelines
```bash
./scripts/test-pipelines.sh
```
Creates and executes all 6 example pipelines.

### 5. Complete Test Suite
```bash
./scripts/run-all-tests.sh
```
Runs all tests in sequence and generates a detailed report.

---

## 🎯 Manual Testing Commands

### Start/Stop Services
```bash
# Start all
docker-compose up -d

# Stop all
docker-compose down

# Restart one service
docker-compose restart postgres

# View logs
docker logs river-api -f
```

### Test Single Connector
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

### Create Pipeline
```bash
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/postgres-to-redis-cache-pipeline.json
```

### Start Pipeline
```bash
curl -X POST http://localhost:8080/api/v1/pipelines/postgres-to-redis-cache/start
```

### Check Pipeline Status
```bash
curl http://localhost:8080/api/v1/pipelines/postgres-to-redis-cache
```

### List All Pipelines
```bash
curl http://localhost:8080/api/v1/pipelines
```

---

## 🔍 Verify Data in Each Service

### PostgreSQL
```bash
docker exec -it river-postgres psql -U river -d river_metadata -c "SELECT * FROM test_users;"
```

### MySQL
```bash
docker exec -it river-mysql mysql -uriver -priver_pass -e "USE river_test; SELECT * FROM products;"
```

### MongoDB
```bash
docker exec -it river-mongodb mongosh -u river -p river_pass --eval "use river_test; db.customers.find().pretty();"
```

### Redis
```bash
docker exec -it river-redis redis-cli
> KEYS *
> GET user:1
```

### Kafka
```bash
# List topics
docker exec river-kafka kafka-topics --bootstrap-server localhost:29092 --list

# Consume messages
docker exec river-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic user-events \
  --from-beginning \
  --max-messages 10
```

### Elasticsearch
```bash
# Cluster health
curl http://localhost:9200/_cluster/health?pretty

# Search documents
curl http://localhost:9200/test-logs/_search?pretty

# Document count
curl http://localhost:9200/test-logs/_count
```

---

## 📊 Monitoring & Metrics

### River API Health
```bash
curl http://localhost:8080/actuator/health
```

### Prometheus Metrics
```bash
# UI
open http://localhost:9091

# Query API
curl 'http://localhost:9091/api/v1/query?query=river_pipeline_records_processed_total'
```

### Grafana Dashboards
```bash
open http://localhost:3000
# Login: admin / admin
```

---

## 🧪 Test Scenarios

### Scenario 1: Database to Cache (PostgreSQL → Redis)
```bash
# 1. Insert data in PostgreSQL
docker exec river-postgres psql -U river -d river_metadata -c \
  "INSERT INTO test_users (name, email) VALUES ('Test User', 'test@example.com');"

# 2. Run pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/postgres-to-redis-cache/start

# 3. Verify in Redis
docker exec river-redis redis-cli KEYS "*"
```

### Scenario 2: NoSQL to Search (MongoDB → Elasticsearch)
```bash
# 1. Insert data in MongoDB
docker exec river-mongodb mongosh -u river -p river_pass --eval \
  "use river_test; db.customers.insertOne({name: 'New Customer', email: 'new@example.com', status: 'active'});"

# 2. Run pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/mongodb-to-elasticsearch-sync/start

# 3. Verify in Elasticsearch
curl http://localhost:9200/customers/_search?pretty
```

### Scenario 3: Streaming (MySQL → Kafka → MongoDB)
```bash
# 1. Start streaming pipelines
curl -X POST http://localhost:8080/api/v1/pipelines/mysql-to-kafka-cdc/start
curl -X POST http://localhost:8080/api/v1/pipelines/kafka-to-mongodb-events/start

# 2. Insert data in MySQL
docker exec river-mysql mysql -uriver -priver_pass -e \
  "USE river_test; INSERT INTO products (name, price) VALUES ('New Product', 199.99);"

# 3. Verify in Kafka
docker exec river-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic mysql-changes \
  --from-beginning --max-messages 1

# 4. Verify in MongoDB
docker exec river-mongodb mongosh -u river -p river_pass --eval \
  "use river_test; db.events.find().sort({_id:-1}).limit(1).pretty();"
```

---

## 🐛 Troubleshooting

### Services Not Starting
```bash
# Check status
docker-compose ps

# Check logs
docker logs river-postgres
docker logs river-mysql
docker logs river-api

# Restart service
docker-compose restart <service-name>
```

### Connection Refused
```bash
# Test network connectivity
docker exec river-api ping postgres
docker exec river-api ping mysql

# Check network
docker network inspect river_river-network
```

### Pipeline Execution Failed
```bash
# Check API logs
docker logs river-api | grep ERROR

# Check pipeline status
curl http://localhost:8080/api/v1/pipelines/<pipeline-name>

# Check executions
curl http://localhost:8080/api/v1/pipelines/<pipeline-name>/executions
```

### Data Not Flowing
```bash
# 1. Verify source has data
docker exec -it river-postgres psql -U river -d river_metadata -c "SELECT COUNT(*) FROM test_users;"

# 2. Test source connector
curl -X POST http://localhost:8080/api/v1/connectors/test -d @source-config.json

# 3. Test destination connector
curl -X POST http://localhost:8080/api/v1/connectors/test -d @dest-config.json

# 4. Check pipeline config
curl http://localhost:8080/api/v1/pipelines/<pipeline-name>
```

---

## 🎓 Example Pipelines

All examples are in `/examples` directory:

| Pipeline | Source | Destination | Use Case |
|----------|--------|-------------|----------|
| `postgres-to-redis-cache-pipeline.json` | PostgreSQL | Redis | Caching |
| `mongodb-to-elasticsearch-pipeline.json` | MongoDB | Elasticsearch | Search indexing |
| `mysql-to-kafka-pipeline.json` | MySQL | Kafka | CDC streaming |
| `kafka-to-mongodb-pipeline.json` | Kafka | MongoDB | Event storage |
| `elasticsearch-to-mysql-pipeline.json` | Elasticsearch | MySQL | Reporting |
| `postgres-to-postgres-pipeline.json` | PostgreSQL | PostgreSQL | Replication |
| `s3-to-postgres-pipeline.json` | S3 | PostgreSQL | Data ingestion |

---

## 📖 Documentation

- **Complete Guide**: `TESTING_GUIDE.md` - Comprehensive testing instructions
- **Connector Summary**: `CONNECTORS_SUMMARY.md` - All connectors at a glance
- **Connector Details**: `docs/connectors-list.md` - Detailed connector configurations
- **Architecture**: `docs/architecture.md` - Platform design and internals
- **Development**: `docs/connector-development.md` - Build custom connectors
- **Quick Start**: `docs/quickstart.md` - Getting started guide

---

## 🎯 Checklist

Quick checklist for testing:

- [ ] All services running (`docker-compose ps`)
- [ ] All services healthy (`./scripts/verify-services.sh`)
- [ ] Test data loaded (`./scripts/setup-test-data.sh`)
- [ ] All connectors tested (`./scripts/test-connectors.sh`)
- [ ] All pipelines tested (`./scripts/test-pipelines.sh`)
- [ ] Metrics visible (http://localhost:9091)
- [ ] Dashboards accessible (http://localhost:3000)
- [ ] API responding (http://localhost:8080/actuator/health)

---

## 💡 Quick Tips

1. **Always wait 60 seconds** after `docker-compose up` for services to initialize
2. **Run setup-test-data.sh** before testing pipelines
3. **Check logs** if something fails: `docker logs <service-name>`
4. **Use verify-services.sh** to check service health
5. **Test one connector at a time** when debugging
6. **Grafana password**: admin/admin (change on first login)
7. **All services** use credentials: river/river_pass

---

## 🚀 Next Steps

After successful testing:

1. ✅ Explore custom pipeline configurations
2. ✅ Build custom connectors (see `docs/connector-development.md`)
3. ✅ Set up production deployment with Kubernetes
4. ✅ Configure monitoring and alerting
5. ✅ Add data transformations and processors
6. ✅ Scale horizontally with distributed workers

---

**Happy Testing! 🎉**
