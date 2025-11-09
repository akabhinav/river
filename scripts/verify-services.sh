#!/bin/bash

# River Platform - Service Verification Script
# This script verifies that all services are running and accessible

set -e

echo "=========================================="
echo "River Platform - Service Verification"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if service is responsive
check_service() {
    local service_name=$1
    local check_command=$2

    echo -n "Checking $service_name... "

    if eval "$check_command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC}"
        return 1
    fi
}

# Counter for failed services
FAILED=0

echo "1. Checking Docker Services Status"
echo "-----------------------------------"
docker-compose ps
echo ""

echo "2. Verifying Individual Services"
echo "---------------------------------"

# PostgreSQL
if check_service "PostgreSQL" "docker exec river-postgres pg_isready -U river"; then
    echo "   Database: river_metadata"
else
    ((FAILED++))
fi

# MySQL
if check_service "MySQL" "docker exec river-mysql mysqladmin -uriver -priver_pass ping"; then
    echo "   Database: river_test"
else
    ((FAILED++))
fi

# MongoDB
if check_service "MongoDB" "docker exec river-mongodb mongosh -u river -p river_pass --quiet --eval 'db.adminCommand({ ping: 1 })'"; then
    echo "   Database: river_test"
else
    ((FAILED++))
fi

# Redis
if check_service "Redis" "docker exec river-redis redis-cli PING"; then
    echo "   Connected successfully"
else
    ((FAILED++))
fi

# Kafka
if check_service "Kafka" "docker exec river-kafka kafka-broker-api-versions --bootstrap-server localhost:29092"; then
    echo "   Bootstrap servers: kafka:29092"
else
    ((FAILED++))
fi

# Elasticsearch
if check_service "Elasticsearch" "curl -s http://localhost:9200/_cluster/health"; then
    echo "   Cluster health: $(curl -s http://localhost:9200/_cluster/health | grep -o '\"status\":\"[^\"]*\"')"
else
    ((FAILED++))
fi

# Prometheus
if check_service "Prometheus" "curl -s http://localhost:9091/-/healthy"; then
    echo "   Metrics endpoint available"
else
    ((FAILED++))
fi

# Grafana
if check_service "Grafana" "curl -s http://localhost:3000/api/health"; then
    echo "   Dashboard UI: http://localhost:3000"
else
    ((FAILED++))
fi

# River API
if check_service "River API" "curl -s http://localhost:8080/actuator/health"; then
    echo "   API endpoint: http://localhost:8080"
    echo "   Health: $(curl -s http://localhost:8080/actuator/health | grep -o '\"status\":\"[^\"]*\"')"
else
    ((FAILED++))
fi

echo ""
echo "3. Service Endpoints Summary"
echo "----------------------------"
echo "PostgreSQL:     localhost:5432 (user: river, db: river_metadata)"
echo "MySQL:          localhost:3306 (user: river, db: river_test)"
echo "MongoDB:        localhost:27017 (user: river, db: river_test)"
echo "Redis:          localhost:6379"
echo "Kafka:          localhost:9092"
echo "Elasticsearch:  http://localhost:9200"
echo "Prometheus:     http://localhost:9091"
echo "Grafana:        http://localhost:3000 (admin/admin)"
echo "River API:      http://localhost:8080"

echo ""
echo "=========================================="
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All services are running successfully!${NC}"
    echo "=========================================="
    exit 0
else
    echo -e "${RED}✗ $FAILED service(s) failed verification${NC}"
    echo "=========================================="
    echo ""
    echo "Troubleshooting tips:"
    echo "1. Run 'docker-compose logs <service-name>' to check logs"
    echo "2. Ensure all required ports are available"
    echo "3. Wait 60 seconds after 'docker-compose up' for initialization"
    echo "4. Run 'docker-compose restart <service-name>' to restart failed service"
    exit 1
fi
