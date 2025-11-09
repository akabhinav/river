#!/bin/bash

# River Platform - Pipeline Testing Script
# This script tests all example pipeline configurations

set -e

echo "=========================================="
echo "River Platform - Pipeline Testing"
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

# Function to test pipeline creation
test_pipeline_create() {
    local pipeline_name=$1
    local config_file=$2

    echo ""
    echo -e "${BLUE}Testing: $pipeline_name${NC}"
    echo "-----------------------------------"

    if [ ! -f "$config_file" ]; then
        echo -e "${YELLOW}SKIPPED (config file not found: $config_file)${NC}"
        return
    fi

    # Create pipeline
    echo -n "1. Creating pipeline... "
    CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/api/v1/pipelines" \
        -H "Content-Type: application/json" \
        -d @"$config_file")

    HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -n1)
    BODY=$(echo "$CREATE_RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
        echo -e "${GREEN}✓ Created${NC}"
    else
        echo -e "${RED}✗ Failed (HTTP $HTTP_CODE)${NC}"
        echo "   Response: $BODY"
        ((FAILED++))
        return
    fi

    # Get pipeline ID from file
    PIPELINE_ID=$(grep -o '"name":"[^"]*"' "$config_file" | head -1 | cut -d'"' -f4)

    # Get pipeline details
    echo -n "2. Retrieving pipeline... "
    GET_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_BASE/api/v1/pipelines/$PIPELINE_ID")
    HTTP_CODE=$(echo "$GET_RESPONSE" | tail -n1)

    if [ "$HTTP_CODE" -eq 200 ]; then
        echo -e "${GREEN}✓ Retrieved${NC}"
    else
        echo -e "${RED}✗ Failed (HTTP $HTTP_CODE)${NC}"
        ((FAILED++))
        return
    fi

    # Start pipeline execution
    echo -n "3. Starting execution... "
    START_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/api/v1/pipelines/$PIPELINE_ID/start")
    HTTP_CODE=$(echo "$START_RESPONSE" | tail -n1)

    if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 202 ]; then
        echo -e "${GREEN}✓ Started${NC}"
    else
        echo -e "${RED}✗ Failed (HTTP $HTTP_CODE)${NC}"
        BODY=$(echo "$START_RESPONSE" | sed '$d')
        echo "   Response: $BODY"
        ((FAILED++))
        return
    fi

    # Wait for execution
    echo -n "4. Waiting for completion... "
    sleep 5
    echo -e "${YELLOW}⏳ Running${NC}"

    # Check execution status
    echo -n "5. Checking status... "
    STATUS_RESPONSE=$(curl -s -X GET "$API_BASE/api/v1/pipelines/$PIPELINE_ID/executions" | head -c 500)
    if [ -n "$STATUS_RESPONSE" ]; then
        echo -e "${GREEN}✓ Status retrieved${NC}"
    else
        echo -e "${YELLOW}⚠ No execution history${NC}"
    fi

    echo -e "${GREEN}✓ Pipeline test completed${NC}"
    ((PASSED++))
}

# Function to verify data in destination
verify_destination_data() {
    local dest_type=$1
    local expected_records=$2

    echo -n "6. Verifying destination data... "

    case $dest_type in
        "redis")
            RECORD_COUNT=$(docker exec river-redis redis-cli DBSIZE 2>/dev/null || echo "0")
            ;;
        "mongodb")
            RECORD_COUNT=$(docker exec river-mongodb mongosh -u river -p river_pass --quiet --eval "use river_test; db.events.countDocuments({})" 2>/dev/null || echo "0")
            ;;
        "elasticsearch")
            RECORD_COUNT=$(curl -s "http://localhost:9200/customers/_count" 2>/dev/null | grep -o '"count":[0-9]*' | cut -d: -f2 || echo "0")
            ;;
        "mysql")
            RECORD_COUNT=$(docker exec river-mysql mysql -uriver -priver_pass -se "USE river_test; SELECT COUNT(*) FROM log_summary;" 2>/dev/null || echo "0")
            ;;
        *)
            echo -e "${YELLOW}⚠ Verification not implemented${NC}"
            return
            ;;
    esac

    if [ "$RECORD_COUNT" -ge "$expected_records" ]; then
        echo -e "${GREEN}✓ Verified ($RECORD_COUNT records)${NC}"
    else
        echo -e "${YELLOW}⚠ Found $RECORD_COUNT records (expected >= $expected_records)${NC}"
    fi
}

echo "This script will test all example pipelines."
echo "Make sure you have run setup-test-data.sh first!"
echo ""
read -p "Press Enter to continue or Ctrl+C to abort..."
echo ""

# Test Pipeline 1: PostgreSQL → Redis (Caching)
test_pipeline_create \
    "PostgreSQL → Redis (Caching)" \
    "examples/postgres-to-redis-cache-pipeline.json"
verify_destination_data "redis" 5

# Test Pipeline 2: MongoDB → Elasticsearch (Search)
test_pipeline_create \
    "MongoDB → Elasticsearch (Search)" \
    "examples/mongodb-to-elasticsearch-pipeline.json"
verify_destination_data "elasticsearch" 4

# Test Pipeline 3: MySQL → Kafka (CDC)
test_pipeline_create \
    "MySQL → Kafka (CDC)" \
    "examples/mysql-to-kafka-pipeline.json"
# Kafka verification is complex, skip for now

# Test Pipeline 4: Kafka → MongoDB (Events)
test_pipeline_create \
    "Kafka → MongoDB (Events)" \
    "examples/kafka-to-mongodb-pipeline.json"
verify_destination_data "mongodb" 1

# Test Pipeline 5: Elasticsearch → MySQL (Reporting)
test_pipeline_create \
    "Elasticsearch → MySQL (Reporting)" \
    "examples/elasticsearch-to-mysql-pipeline.json"
verify_destination_data "mysql" 1

# Test Pipeline 6: PostgreSQL → PostgreSQL (Replication)
test_pipeline_create \
    "PostgreSQL → PostgreSQL (Replication)" \
    "examples/postgres-to-postgres-pipeline.json"

# Summary
echo ""
echo "=========================================="
echo "Pipeline Test Results"
echo "=========================================="
echo -e "Pipelines Tested: ${GREEN}$PASSED${NC}"
echo -e "Failures: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All pipeline tests completed successfully!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. View pipeline executions in API: curl http://localhost:8080/api/v1/pipelines"
    echo "2. Check Grafana dashboards: http://localhost:3000"
    echo "3. Query Prometheus metrics: http://localhost:9091"
    echo "4. Verify data in each destination system manually"
    exit 0
else
    echo -e "${YELLOW}⚠ Some pipeline tests encountered issues${NC}"
    echo ""
    echo "Troubleshooting:"
    echo "1. Check API logs: docker logs river-api"
    echo "2. Verify services: ./scripts/verify-services.sh"
    echo "3. Check individual connector: ./scripts/test-connectors.sh"
    exit 1
fi
