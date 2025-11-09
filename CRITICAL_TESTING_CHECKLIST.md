# River Platform - Critical Testing Checklist

This checklist covers the essential tests that MUST be performed with real data to validate the platform.

## ⚠️ Important Note

**The test scripts have been created but NOT executed with real data yet.**

You must run these tests on a machine with Docker installed to validate everything works correctly.

---

## 🔴 Critical Tests (Must Pass)

### Test 1: All Services Start Successfully

```bash
# Start services
docker-compose up -d

# Wait for initialization
sleep 60

# Verify all services are healthy
./scripts/verify-services.sh
```

**Expected Result:**
- ✅ All 10 services show "Up" status
- ✅ PostgreSQL responds to pg_isready
- ✅ MySQL responds to ping
- ✅ MongoDB accepts connections
- ✅ Redis responds to PING
- ✅ Kafka broker is reachable
- ✅ Elasticsearch cluster is healthy
- ✅ River API returns 200 on /actuator/health
- ✅ Prometheus is running
- ✅ Grafana is accessible

**If This Fails:**
- Check `docker-compose ps` for failed services
- Review logs: `docker logs <service-name>`
- Ensure all required ports are available
- Verify sufficient system resources (8GB+ RAM)

---

### Test 2: Test Data Creation

```bash
# Create test data in all systems
./scripts/setup-test-data.sh
```

**Expected Result:**
- ✅ PostgreSQL: 5 users, 5 orders inserted
- ✅ MySQL: 8 products inserted
- ✅ MongoDB: 4 customers, 2 events inserted
- ✅ Redis: 15+ keys created
- ✅ Kafka: 3 topics created with messages
- ✅ Elasticsearch: 6 log documents indexed

**Manual Verification:**

```bash
# PostgreSQL
docker exec -it river-postgres psql -U river -d river_metadata -c "SELECT COUNT(*) FROM test_users;"
# Expected: 5

# MySQL
docker exec -it river-mysql mysql -uriver -priver_pass -e "USE river_test; SELECT COUNT(*) FROM products;"
# Expected: 8

# MongoDB
docker exec -it river-mongodb mongosh -u river -p river_pass --eval "use river_test; db.customers.countDocuments({})"
# Expected: 4

# Redis
docker exec -it river-redis redis-cli DBSIZE
# Expected: 15+

# Elasticsearch
curl -s http://localhost:9200/test-logs/_count | grep -o '"count":[0-9]*'
# Expected: count:6
```

**If This Fails:**
- Service may not be fully initialized
- Check credentials in docker-compose.yml
- Verify network connectivity between containers

---

### Test 3: Individual Connector Tests

```bash
# Test all connectors
./scripts/test-connectors.sh
```

**Expected Result:**
Each connector should return HTTP 200/201:

**Source Connectors:**
- ✅ PostgreSQL Source - Can read from test_users table
- ✅ MySQL Source - Can read from products table
- ✅ MongoDB Source - Can read from customers collection
- ✅ Redis Source - Can scan keys with pattern
- ✅ Kafka Source - Can consume from topics
- ✅ Elasticsearch Source - Can scroll through documents

**Destination Connectors:**
- ✅ PostgreSQL Destination - Can accept write operations
- ✅ MySQL Destination - Can accept write operations
- ✅ MongoDB Destination - Can accept write operations
- ✅ Redis Destination - Can accept write operations
- ✅ Kafka Destination - Can produce messages
- ✅ Elasticsearch Destination - Can bulk index

**If This Fails:**
- Check River API logs: `docker logs river-api`
- Verify connector configuration in test script
- Test individual service connectivity
- Check if River API loaded connector classes correctly

---

### Test 4: PostgreSQL → Redis Pipeline (Caching)

```bash
# Create and execute pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/postgres-to-redis-cache-pipeline.json

curl -X POST http://localhost:8080/api/v1/pipelines/postgres-to-redis-cache/start

# Wait for execution
sleep 10
```

**Verify Data Flow:**

```bash
# Check source data exists
docker exec -it river-postgres psql -U river -d river_metadata -c \
  "SELECT id, name, email FROM test_users WHERE active = true;"

# Verify data in Redis
docker exec -it river-redis redis-cli KEYS "*"
docker exec -it river-redis redis-cli GET <some-key>

# Expected: Data from PostgreSQL should appear in Redis with correct TTL
docker exec -it river-redis redis-cli TTL <some-key>
# Expected: TTL around 3600 seconds
```

**Critical Success Criteria:**
- ✅ Pipeline created without errors (HTTP 200/201)
- ✅ Pipeline execution started (HTTP 200/202)
- ✅ Data appears in Redis
- ✅ Key count matches active users in PostgreSQL
- ✅ TTL is set correctly (3600 seconds)
- ✅ Data values match source data

**If This Fails:**
- Most critical test - indicates fundamental platform issue
- Check API logs for connector errors
- Verify pipeline configuration is correct
- Test source and destination connectors individually first
- Check if data types are compatible

---

### Test 5: MongoDB → Elasticsearch Pipeline (Search)

```bash
# Create and execute pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/mongodb-to-elasticsearch-pipeline.json

curl -X POST http://localhost:8080/api/v1/pipelines/mongodb-to-elasticsearch-sync/start

# Wait for execution
sleep 10
```

**Verify Data Flow:**

```bash
# Check source data
docker exec -it river-mongodb mongosh -u river -p river_pass --eval \
  "use river_test; db.customers.find({status:'active'}).count()"
# Expected: 3 active customers

# Verify data in Elasticsearch
curl -s http://localhost:9200/customers/_count | grep -o '"count":[0-9]*'
# Expected: count:3

# Search for specific customer
curl -s http://localhost:9200/customers/_search?q=name:John | grep -o '"total":{"value":[0-9]*'
# Expected: Should find John Doe
```

**Critical Success Criteria:**
- ✅ Pipeline executed successfully
- ✅ Document count matches (3 active customers)
- ✅ Documents are searchable in Elasticsearch
- ✅ Field mapping is correct
- ✅ Data values preserved correctly

---

### Test 6: MySQL → Kafka Pipeline (CDC)

```bash
# Create and execute pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/mysql-to-kafka-pipeline.json

curl -X POST http://localhost:8080/api/v1/pipelines/mysql-to-kafka-cdc/start

# Wait for pipeline to start streaming
sleep 5
```

**Verify Streaming:**

```bash
# Insert new data in MySQL
docker exec -it river-mysql mysql -uriver -priver_pass -e \
  "USE river_test; INSERT INTO products (name, price) VALUES ('Test Product', 123.45);"

# Wait for message to propagate
sleep 3

# Consume from Kafka to verify message
docker exec -it river-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic mysql-changes \
  --from-beginning \
  --max-messages 1
```

**Critical Success Criteria:**
- ✅ Pipeline starts in STREAMING mode
- ✅ Messages appear in Kafka topic
- ✅ Message contains correct product data
- ✅ New inserts trigger new messages
- ✅ Streaming continues without stopping

**If This Fails:**
- Streaming is complex - check consumer group settings
- Verify Kafka topic was created correctly
- Check if MySQL connector can read from table
- Ensure pipeline mode is set to "STREAMING"

---

### Test 7: Kafka → MongoDB Pipeline (Events)

```bash
# Create and execute pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/kafka-to-mongodb-pipeline.json

curl -X POST http://localhost:8080/api/v1/pipelines/kafka-to-mongodb-events/start

# Produce test message to Kafka
echo '{"event":"test","user":"alice","timestamp":"'$(date -Iseconds)'"}' | \
  docker exec -i river-kafka kafka-console-producer \
    --bootstrap-server localhost:29092 \
    --topic user-events

# Wait for processing
sleep 5
```

**Verify Data Flow:**

```bash
# Check if event appeared in MongoDB
docker exec -it river-mongodb mongosh -u river -p river_pass --eval \
  "use river_test; db.events.find().sort({_id:-1}).limit(1).pretty()"
# Expected: Should see the test event
```

**Critical Success Criteria:**
- ✅ Pipeline consumes from Kafka
- ✅ Events stored in MongoDB
- ✅ Data structure preserved
- ✅ Continuous streaming works
- ✅ No message loss

---

### Test 8: Elasticsearch → MySQL Pipeline (Reporting)

```bash
# Create and execute pipeline
curl -X POST http://localhost:8080/api/v1/pipelines \
  -H "Content-Type: application/json" \
  -d @examples/elasticsearch-to-mysql-pipeline.json

curl -X POST http://localhost:8080/api/v1/pipelines/elasticsearch-to-mysql-reporting/start

# Wait for execution
sleep 10
```

**Verify Data Flow:**

```bash
# Check source data in Elasticsearch
curl -s http://localhost:9200/test-logs/_count | grep -o '"count":[0-9]*'
# Expected: 6 log documents

# Verify data in MySQL
docker exec -it river-mysql mysql -uriver -priver_pass -e \
  "USE river_test; SELECT COUNT(*) FROM log_summary;"
# Expected: Should have records from Elasticsearch query
```

**Critical Success Criteria:**
- ✅ Pipeline executes query on Elasticsearch
- ✅ Results stored in MySQL
- ✅ Data transformation works correctly
- ✅ Schedule executes if configured

---

## 🟡 Important Tests (Should Pass)

### Test 9: Multi-Hop Data Flow

**Test the complete flow: PostgreSQL → MongoDB → Elasticsearch**

```bash
# 1. Insert data in PostgreSQL
docker exec -it river-postgres psql -U river -d river_metadata -c \
  "INSERT INTO test_users (name, email) VALUES ('Multi-Hop Test', 'multihop@test.com');"

# 2. Create PostgreSQL → MongoDB pipeline
curl -X POST http://localhost:8080/api/v1/pipelines -H "Content-Type: application/json" -d '{
  "name": "postgres-to-mongo-test",
  "mode": "BATCH",
  "source": {
    "type": "postgresql-source",
    "connection": {
      "host": "postgres",
      "port": 5432,
      "database": "river_metadata",
      "username": "river",
      "password": "river_pass",
      "query": "SELECT * FROM test_users WHERE email = '\''multihop@test.com'\''"
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

# 3. Execute first pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/postgres-to-mongo-test/start
sleep 5

# 4. Verify in MongoDB
docker exec -it river-mongodb mongosh -u river -p river_pass --eval \
  "use river_test; db.users_from_postgres.find({email:'multihop@test.com'}).pretty()"

# 5. Create MongoDB → Elasticsearch pipeline
curl -X POST http://localhost:8080/api/v1/pipelines -H "Content-Type: application/json" -d '{
  "name": "mongo-to-es-test",
  "mode": "BATCH",
  "source": {
    "type": "mongodb-source",
    "connection": {
      "uri": "mongodb://river:river_pass@mongodb:27017",
      "database": "river_test",
      "collection": "users_from_postgres",
      "filter": "{\"email\":\"multihop@test.com\"}"
    }
  },
  "destination": {
    "type": "elasticsearch-destination",
    "connection": {
      "host": "elasticsearch",
      "port": 9200,
      "index": "users-test"
    }
  }
}'

# 6. Execute second pipeline
curl -X POST http://localhost:8080/api/v1/pipelines/mongo-to-es-test/start
sleep 5

# 7. Verify in Elasticsearch
curl -s http://localhost:9200/users-test/_search?q=email:multihop@test.com | grep multihop
```

**Success Criteria:**
- ✅ Data flows through all 3 systems
- ✅ Data integrity maintained
- ✅ All transformations work correctly

---

### Test 10: Error Handling

**Test what happens when things fail:**

```bash
# 1. Test with invalid credentials
curl -X POST http://localhost:8080/api/v1/pipelines -H "Content-Type: application/json" -d '{
  "name": "error-test",
  "mode": "BATCH",
  "source": {
    "type": "postgresql-source",
    "connection": {
      "host": "postgres",
      "port": 5432,
      "database": "river_metadata",
      "username": "invalid_user",
      "password": "wrong_password",
      "query": "SELECT * FROM test_users"
    }
  },
  "destination": {
    "type": "redis-destination",
    "connection": {
      "host": "redis",
      "port": 6379
    }
  }
}'

# Expected: Should fail gracefully with clear error message

# 2. Test with non-existent table
curl -X POST http://localhost:8080/api/v1/pipelines -H "Content-Type: application/json" -d '{
  "name": "error-test-2",
  "mode": "BATCH",
  "source": {
    "type": "postgresql-source",
    "connection": {
      "host": "postgres",
      "port": 5432,
      "database": "river_metadata",
      "username": "river",
      "password": "river_pass",
      "query": "SELECT * FROM non_existent_table"
    }
  },
  "destination": {
    "type": "redis-destination",
    "connection": {
      "host": "redis",
      "port": 6379
    }
  }
}'

# Expected: Should fail with appropriate error message
```

**Success Criteria:**
- ✅ Errors are caught and reported clearly
- ✅ System doesn't crash
- ✅ Error messages are helpful
- ✅ Failed pipelines can be deleted/retried

---

### Test 11: Performance (Large Dataset)

```bash
# 1. Create large dataset in PostgreSQL
docker exec -it river-postgres psql -U river -d river_metadata << 'EOF'
INSERT INTO test_users (name, email)
SELECT
  'User ' || generate_series,
  'user' || generate_series || '@example.com'
FROM generate_series(1, 100000);
EOF

# 2. Time the transfer to Redis
time curl -X POST http://localhost:8080/api/v1/pipelines/postgres-to-redis-cache/start

# 3. Check metrics
curl http://localhost:9091/api/v1/query?query=river_pipeline_execution_duration_seconds
```

**Success Criteria:**
- ✅ Can handle 100K+ records
- ✅ No memory leaks
- ✅ Reasonable performance (< 5 minutes for 100K records)
- ✅ Metrics are tracked correctly

---

### Test 12: Monitoring & Metrics

```bash
# 1. Check Prometheus metrics
curl http://localhost:9091/api/v1/query?query=river_pipeline_records_processed_total

# 2. Access Grafana
open http://localhost:3000
# Login: admin/admin

# 3. Check API metrics
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/river.connector.read.duration
```

**Success Criteria:**
- ✅ Prometheus scrapes River API metrics
- ✅ Grafana can connect to Prometheus
- ✅ Metrics show accurate counts
- ✅ Can create dashboards

---

## 📋 Final Checklist

Before considering the platform production-ready:

### Core Functionality
- [ ] All 10 services start successfully
- [ ] Test data creation works
- [ ] All 13 connectors pass individual tests
- [ ] At least 5 out of 7 example pipelines work
- [ ] Error handling works correctly

### Data Integrity
- [ ] PostgreSQL → Redis: Data matches exactly
- [ ] MongoDB → Elasticsearch: All fields preserved
- [ ] MySQL → Kafka: Messages contain correct data
- [ ] Kafka → MongoDB: No message loss
- [ ] Multi-hop: Data flows through multiple systems correctly

### Performance
- [ ] Can handle 100K+ records
- [ ] Streaming pipelines run continuously
- [ ] No memory leaks in long-running pipelines
- [ ] Batch pipelines complete in reasonable time

### Monitoring
- [ ] Prometheus collects metrics
- [ ] Grafana displays dashboards
- [ ] API health endpoint responds
- [ ] Logs are accessible and useful

### Reliability
- [ ] Pipelines can be stopped and restarted
- [ ] Service restarts don't cause data loss
- [ ] Connection failures are handled gracefully
- [ ] Failed pipelines provide clear error messages

---

## 🚨 Known Limitations to Test

These are potential issues to watch for:

1. **Maven Build** - May fail if dependencies can't be downloaded
   - Test: Can the project build with `mvn clean package`?

2. **Network Issues** - Containers may not communicate properly
   - Test: Can River API connect to all data sources?

3. **Resource Limits** - May need more RAM for all services
   - Test: Do all services stay running under load?

4. **Port Conflicts** - Required ports may already be in use
   - Test: Do all services bind to their ports successfully?

5. **Data Type Mapping** - Complex types may not translate correctly
   - Test: Do JSON fields, arrays, nested objects work?

---

## 📊 Results Template

Track your results:

```markdown
## Test Results - [Date]

### Critical Tests
- [ ] ✅/❌ All services started
- [ ] ✅/❌ Test data created
- [ ] ✅/❌ All connectors tested
- [ ] ✅/❌ PostgreSQL → Redis works
- [ ] ✅/❌ MongoDB → Elasticsearch works
- [ ] ✅/❌ MySQL → Kafka works
- [ ] ✅/❌ Kafka → MongoDB works
- [ ] ✅/❌ Elasticsearch → MySQL works

### Important Tests
- [ ] ✅/❌ Multi-hop data flow
- [ ] ✅/❌ Error handling
- [ ] ✅/❌ Performance (100K records)
- [ ] ✅/❌ Monitoring & metrics

### Issues Found
1. [Description of issue]
2. [Description of issue]

### Notes
[Additional observations]
```

---

## 🎯 Next Steps After Testing

If all tests pass:
1. ✅ Document any configuration changes needed
2. ✅ Create production deployment plan
3. ✅ Set up CI/CD pipeline
4. ✅ Configure production monitoring
5. ✅ Plan for additional connectors

If tests fail:
1. ❌ Document failures in detail
2. ❌ Check logs for root causes
3. ❌ Fix code issues
4. ❌ Re-test
5. ❌ Update documentation with findings

---

**This checklist must be completed before the platform can be considered production-ready.**
