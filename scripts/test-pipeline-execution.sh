#!/bin/bash

# Script to test end-to-end pipeline execution

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

echo "========================================="
echo "  River Platform Pipeline E2E Test"
echo "========================================="
echo ""

# Check if services are running
print_info "Checking if services are running..."
if ! docker-compose ps | grep -q "Up"; then
    print_error "Services not running. Start with: docker-compose up -d"
    exit 1
fi
print_success "Services are running"

# Check API health
print_info "Checking API health..."
if ! curl -sf http://localhost:8080/actuator/health > /dev/null; then
    print_error "API is not healthy"
    exit 1
fi
print_success "API is healthy"

# Create test data
print_info "Creating test data..."
bash scripts/create-test-data.sh > /dev/null 2>&1
print_success "Test data created"

# Create pipeline
print_info "Creating pipeline..."
PIPELINE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/pipelines \
    -H "Content-Type: application/json" \
    -d '{
        "name": "e2e-test-pipeline",
        "description": "End-to-end test pipeline",
        "mode": "BATCH",
        "source": {
            "type": "postgresql-source",
            "connection": {
                "host": "postgres",
                "port": 5432,
                "database": "river_metadata",
                "username": "river",
                "password": "river_pass",
                "query": "SELECT * FROM test_source.users WHERE status = '\''active'\''"
            },
            "readMode": "FULL",
            "parallelism": 2
        },
        "destination": {
            "type": "postgresql-destination",
            "connection": {
                "host": "postgres",
                "port": 5432,
                "database": "river_metadata",
                "username": "river",
                "password": "river_pass",
                "table": "test_destination.users"
            },
            "writeMode": "APPEND",
            "batchSize": 100,
            "transactional": true
        }
    }')

PIPELINE_ID=$(echo "$PIPELINE_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

if [ -z "$PIPELINE_ID" ]; then
    print_error "Failed to create pipeline"
    echo "$PIPELINE_RESPONSE"
    exit 1
fi

print_success "Pipeline created: $PIPELINE_ID"

# Get pipeline details
print_info "Retrieving pipeline details..."
PIPELINE_DETAILS=$(curl -s http://localhost:8080/api/v1/pipelines/$PIPELINE_ID)
echo "$PIPELINE_DETAILS" | grep -q "e2e-test-pipeline"
print_success "Pipeline details retrieved"

# List all pipelines
print_info "Listing all pipelines..."
ALL_PIPELINES=$(curl -s http://localhost:8080/api/v1/pipelines)
echo "$ALL_PIPELINES" | grep -q "$PIPELINE_ID"
print_success "Pipeline appears in list"

# Note: Actual pipeline execution would require the executor to be implemented
# For now, we'll just verify the pipeline was created and can be retrieved

# Verify source data exists
print_info "Verifying source data..."
SOURCE_COUNT=$(docker exec river-postgres-1 psql -U river -d river_metadata -t -c \
    "SELECT COUNT(*) FROM test_source.users WHERE status = 'active'" | tr -d ' ')
print_success "Source has $SOURCE_COUNT active users"

# Verify destination is ready
print_info "Verifying destination table exists..."
docker exec river-postgres-1 psql -U river -d river_metadata -c \
    "SELECT COUNT(*) FROM test_destination.users" > /dev/null 2>&1
print_success "Destination table is ready"

# Test metrics
print_info "Checking metrics..."
METRICS=$(curl -s http://localhost:8080/actuator/metrics)
echo "$METRICS" | grep -q "jvm.memory.used"
print_success "Metrics are available"

# Test Prometheus endpoint
print_info "Checking Prometheus metrics..."
PROM_METRICS=$(curl -s http://localhost:8080/actuator/prometheus)
echo "$PROM_METRICS" | grep -q "jvm_memory_used_bytes"
print_success "Prometheus metrics are available"

# Cleanup - delete the test pipeline
print_info "Cleaning up test pipeline..."
curl -s -X DELETE http://localhost:8080/api/v1/pipelines/$PIPELINE_ID > /dev/null
print_success "Test pipeline deleted"

# Summary
echo ""
echo "========================================="
echo "  Test Results Summary"
echo "========================================="
echo ""
print_success "All E2E tests passed!"
echo ""
echo "Tested components:"
echo "  ✓ API health check"
echo "  ✓ Pipeline creation"
echo "  ✓ Pipeline retrieval"
echo "  ✓ Pipeline listing"
echo "  ✓ Test data creation"
echo "  ✓ PostgreSQL connectivity"
echo "  ✓ Metrics endpoints"
echo "  ✓ Pipeline deletion"
echo ""
echo "Source data: $SOURCE_COUNT active users in test_source.users"
echo ""
echo "Next steps:"
echo "  • Implement pipeline execution logic"
echo "  • Test actual data movement"
echo "  • Add transformation testing"
echo "  • Test error handling and retries"
echo ""
