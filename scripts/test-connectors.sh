#!/bin/bash

# River Platform - Connector Testing Script
# This script tests all source and destination connectors

set -e

echo "=========================================="
echo "River Platform - Connector Testing"
echo "=========================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

API_BASE="http://localhost:8080"
PASSED=0
FAILED=0

# Function to test connector
test_connector() {
    local connector_name=$1
    local config_file=$2

    echo -n "Testing $connector_name... "

    if [ ! -f "$config_file" ]; then
        echo -e "${YELLOW}SKIPPED (config not found)${NC}"
        return
    fi

    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/api/v1/connectors/test" \
        -H "Content-Type: application/json" \
        -d @"$config_file")

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
        echo -e "${GREEN}✓ PASSED${NC}"
        ((PASSED++))
    else
        echo -e "${RED}✗ FAILED (HTTP $HTTP_CODE)${NC}"
        echo "   Response: $BODY"
        ((FAILED++))
    fi
}

echo -e "${BLUE}Phase 1: Testing Source Connectors${NC}"
echo "======================================"
echo ""

# Test PostgreSQL Source
echo "1. PostgreSQL Source Connector"
cat > /tmp/postgres-source-test.json << 'EOF'
{
  "type": "postgresql-source",
  "connection": {
    "host": "postgres",
    "port": 5432,
    "database": "river_metadata",
    "username": "river",
    "password": "river_pass",
    "query": "SELECT * FROM test_users LIMIT 10"
  },
  "readMode": "FULL"
}
EOF
test_connector "PostgreSQL Source" "/tmp/postgres-source-test.json"
echo ""

# Test MySQL Source
echo "2. MySQL Source Connector"
cat > /tmp/mysql-source-test.json << 'EOF'
{
  "type": "mysql-source",
  "connection": {
    "host": "mysql",
    "port": 3306,
    "database": "river_test",
    "username": "river",
    "password": "river_pass",
    "table": "products"
  },
  "readMode": "FULL"
}
EOF
test_connector "MySQL Source" "/tmp/mysql-source-test.json"
echo ""

# Test MongoDB Source
echo "3. MongoDB Source Connector"
cat > /tmp/mongodb-source-test.json << 'EOF'
{
  "type": "mongodb-source",
  "connection": {
    "uri": "mongodb://river:river_pass@mongodb:27017",
    "database": "river_test",
    "collection": "customers",
    "filter": "{\"status\":\"active\"}",
    "batchSize": 1000
  },
  "readMode": "FULL"
}
EOF
test_connector "MongoDB Source" "/tmp/mongodb-source-test.json"
echo ""

# Test Redis Source
echo "4. Redis Source Connector"
cat > /tmp/redis-source-test.json << 'EOF'
{
  "type": "redis-source",
  "connection": {
    "host": "redis",
    "port": 6379,
    "database": 0,
    "pattern": "user:*",
    "scanCount": 1000
  },
  "readMode": "FULL"
}
EOF
test_connector "Redis Source" "/tmp/redis-source-test.json"
echo ""

# Test Kafka Source
echo "5. Kafka Source Connector"
cat > /tmp/kafka-source-test.json << 'EOF'
{
  "type": "kafka-source",
  "connection": {
    "bootstrapServers": "kafka:29092",
    "topic": "user-events",
    "groupId": "river-test-consumer",
    "autoOffsetReset": "earliest",
    "pollTimeout": 1000
  },
  "readMode": "STREAMING"
}
EOF
test_connector "Kafka Source" "/tmp/kafka-source-test.json"
echo ""

# Test Elasticsearch Source
echo "6. Elasticsearch Source Connector"
cat > /tmp/elasticsearch-source-test.json << 'EOF'
{
  "type": "elasticsearch-source",
  "connection": {
    "host": "elasticsearch",
    "port": 9200,
    "index": "test-logs",
    "query": "{\"query\":{\"match_all\":{}}}",
    "scrollSize": 1000,
    "scrollTimeout": "1m"
  },
  "readMode": "FULL"
}
EOF
test_connector "Elasticsearch Source" "/tmp/elasticsearch-source-test.json"
echo ""

# Test S3 Source (will likely fail without AWS credentials, that's OK)
echo "7. S3 Source Connector"
cat > /tmp/s3-source-test.json << 'EOF'
{
  "type": "s3-source",
  "connection": {
    "bucket": "test-bucket",
    "prefix": "data/",
    "region": "us-east-1",
    "accessKey": "test",
    "secretKey": "test",
    "format": "json"
  },
  "readMode": "FULL"
}
EOF
echo -n "Testing S3 Source... "
echo -e "${YELLOW}SKIPPED (requires AWS credentials)${NC}"
echo ""

echo ""
echo -e "${BLUE}Phase 2: Testing Destination Connectors${NC}"
echo "=========================================="
echo ""

# Test PostgreSQL Destination
echo "8. PostgreSQL Destination Connector"
cat > /tmp/postgres-dest-test.json << 'EOF'
{
  "type": "postgresql-destination",
  "connection": {
    "host": "postgres",
    "port": 5432,
    "database": "river_metadata",
    "username": "river",
    "password": "river_pass",
    "table": "test_users"
  },
  "writeMode": "APPEND",
  "batchSize": 1000,
  "transactional": true
}
EOF
test_connector "PostgreSQL Destination" "/tmp/postgres-dest-test.json"
echo ""

# Test MySQL Destination
echo "9. MySQL Destination Connector"
cat > /tmp/mysql-dest-test.json << 'EOF'
{
  "type": "mysql-destination",
  "connection": {
    "host": "mysql",
    "port": 3306,
    "database": "river_test",
    "username": "river",
    "password": "river_pass",
    "table": "products"
  },
  "writeMode": "APPEND",
  "batchSize": 1000,
  "transactional": true
}
EOF
test_connector "MySQL Destination" "/tmp/mysql-dest-test.json"
echo ""

# Test MongoDB Destination
echo "10. MongoDB Destination Connector"
cat > /tmp/mongodb-dest-test.json << 'EOF'
{
  "type": "mongodb-destination",
  "connection": {
    "uri": "mongodb://river:river_pass@mongodb:27017",
    "database": "river_test",
    "collection": "test_output"
  },
  "writeMode": "APPEND",
  "batchSize": 1000
}
EOF
test_connector "MongoDB Destination" "/tmp/mongodb-dest-test.json"
echo ""

# Test Redis Destination
echo "11. Redis Destination Connector"
cat > /tmp/redis-dest-test.json << 'EOF'
{
  "type": "redis-destination",
  "connection": {
    "host": "redis",
    "port": 6379,
    "database": 0,
    "keyField": "id",
    "valueField": "data",
    "ttl": 3600
  },
  "writeMode": "APPEND",
  "batchSize": 100
}
EOF
test_connector "Redis Destination" "/tmp/redis-dest-test.json"
echo ""

# Test Kafka Destination
echo "12. Kafka Destination Connector"
cat > /tmp/kafka-dest-test.json << 'EOF'
{
  "type": "kafka-destination",
  "connection": {
    "bootstrapServers": "kafka:29092",
    "topic": "test-output",
    "keyField": "id",
    "valueField": "data"
  },
  "writeMode": "APPEND"
}
EOF
test_connector "Kafka Destination" "/tmp/kafka-dest-test.json"
echo ""

# Test Elasticsearch Destination
echo "13. Elasticsearch Destination Connector"
cat > /tmp/elasticsearch-dest-test.json << 'EOF'
{
  "type": "elasticsearch-destination",
  "connection": {
    "host": "elasticsearch",
    "port": 9200,
    "index": "test-output",
    "idField": "_id"
  },
  "writeMode": "APPEND",
  "batchSize": 500
}
EOF
test_connector "Elasticsearch Destination" "/tmp/elasticsearch-dest-test.json"
echo ""

# Summary
echo ""
echo "=========================================="
echo "Test Results Summary"
echo "=========================================="
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
echo "Total:  $((PASSED + FAILED))"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All connector tests passed!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠ Some tests failed. Check the output above for details.${NC}"
    echo ""
    echo "Troubleshooting:"
    echo "1. Ensure all services are running: ./scripts/verify-services.sh"
    echo "2. Ensure test data is loaded: ./scripts/setup-test-data.sh"
    echo "3. Check API logs: docker logs river-api"
    echo "4. Verify connector implementations in code"
    exit 1
fi
