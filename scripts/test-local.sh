#!/bin/bash

# River Platform Local Testing Script
# This script helps you test all components locally

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Function to check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."

    # Check Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
        if [ "$JAVA_VERSION" -ge 21 ]; then
            print_success "Java $JAVA_VERSION found"
        else
            print_error "Java 21 or higher required, found Java $JAVA_VERSION"
            exit 1
        fi
    else
        print_error "Java not found. Please install Java 21+"
        exit 1
    fi

    # Check Maven
    if command -v mvn &> /dev/null; then
        print_success "Maven found"
    else
        print_error "Maven not found. Please install Maven 3.8+"
        exit 1
    fi

    # Check Docker
    if command -v docker &> /dev/null; then
        print_success "Docker found"
    else
        print_error "Docker not found. Please install Docker"
        exit 1
    fi

    # Check Docker Compose
    if command -v docker-compose &> /dev/null; then
        print_success "Docker Compose found"
    else
        print_error "Docker Compose not found. Please install Docker Compose"
        exit 1
    fi
}

# Function to build the project
build_project() {
    print_info "Building River Platform..."

    if mvn clean install -DskipTests; then
        print_success "Build completed successfully"
    else
        print_error "Build failed"
        exit 1
    fi
}

# Function to run unit tests
run_unit_tests() {
    print_info "Running unit tests..."

    if mvn test; then
        print_success "All unit tests passed"
    else
        print_error "Some tests failed"
        exit 1
    fi
}

# Function to start Docker services
start_docker_services() {
    print_info "Starting Docker services..."

    docker-compose up -d

    print_info "Waiting for services to be ready..."
    sleep 15

    # Check if services are running
    if docker-compose ps | grep -q "Up"; then
        print_success "Docker services started"
    else
        print_error "Failed to start Docker services"
        docker-compose logs
        exit 1
    fi
}

# Function to test API health
test_api_health() {
    print_info "Testing API health..."

    MAX_RETRIES=30
    RETRY_COUNT=0

    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            print_success "API is healthy"
            return 0
        fi

        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo -n "."
        sleep 2
    done

    print_error "API health check failed"
    docker-compose logs river
    exit 1
}

# Function to test PostgreSQL
test_postgresql() {
    print_info "Testing PostgreSQL connection..."

    if docker exec river-postgres-1 psql -U river -d river_metadata -c "SELECT 1" > /dev/null 2>&1; then
        print_success "PostgreSQL is accessible"
    else
        print_error "PostgreSQL connection failed"
        exit 1
    fi
}

# Function to test Redis
test_redis() {
    print_info "Testing Redis connection..."

    if docker exec river-redis-1 redis-cli ping | grep -q "PONG"; then
        print_success "Redis is accessible"
    else
        print_error "Redis connection failed"
        exit 1
    fi
}

# Function to test pipeline creation
test_pipeline_creation() {
    print_info "Testing pipeline creation..."

    RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/pipelines \
        -H "Content-Type: application/json" \
        -d '{
            "name": "test-pipeline",
            "description": "Automated test pipeline",
            "mode": "BATCH",
            "source": {
                "type": "postgresql-source",
                "connection": {
                    "host": "postgres",
                    "port": 5432,
                    "database": "river_metadata",
                    "username": "river",
                    "password": "river_pass"
                }
            },
            "destination": {
                "type": "postgresql-destination",
                "connection": {
                    "host": "postgres",
                    "port": 5432,
                    "database": "river_metadata",
                    "username": "river",
                    "password": "river_pass"
                }
            }
        }')

    if echo "$RESPONSE" | grep -q "id"; then
        PIPELINE_ID=$(echo "$RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        print_success "Pipeline created with ID: $PIPELINE_ID"
        echo "$PIPELINE_ID" > /tmp/river_test_pipeline_id
    else
        print_error "Failed to create pipeline"
        echo "$RESPONSE"
        exit 1
    fi
}

# Function to test pipeline retrieval
test_pipeline_retrieval() {
    print_info "Testing pipeline retrieval..."

    if [ -f /tmp/river_test_pipeline_id ]; then
        PIPELINE_ID=$(cat /tmp/river_test_pipeline_id)

        if curl -sf "http://localhost:8080/api/v1/pipelines/$PIPELINE_ID" > /dev/null; then
            print_success "Pipeline retrieved successfully"
        else
            print_error "Failed to retrieve pipeline"
            exit 1
        fi
    else
        print_error "No pipeline ID found"
        exit 1
    fi
}

# Function to test metrics
test_metrics() {
    print_info "Testing metrics endpoint..."

    if curl -sf http://localhost:8080/actuator/prometheus | grep -q "jvm_memory_used_bytes"; then
        print_success "Metrics are available"
    else
        print_error "Metrics endpoint failed"
        exit 1
    fi
}

# Function to show test results
show_results() {
    echo ""
    echo "========================================="
    echo "  River Platform Local Test Results"
    echo "========================================="
    echo ""
    print_success "All tests passed!"
    echo ""
    echo "Access points:"
    echo "  • River API: http://localhost:8080"
    echo "  • Swagger UI: http://localhost:8080/swagger-ui.html"
    echo "  • Health: http://localhost:8080/actuator/health"
    echo "  • Metrics: http://localhost:8080/actuator/prometheus"
    echo "  • Prometheus: http://localhost:9091"
    echo "  • Grafana: http://localhost:3000 (admin/admin)"
    echo ""
    echo "To view logs:"
    echo "  docker-compose logs -f river"
    echo ""
    echo "To stop services:"
    echo "  docker-compose down"
    echo ""
}

# Function to cleanup
cleanup() {
    print_info "Cleaning up test artifacts..."
    rm -f /tmp/river_test_pipeline_id
}

# Main execution
main() {
    echo ""
    echo "========================================="
    echo "  River Platform Local Testing"
    echo "========================================="
    echo ""

    # Parse command line arguments
    MODE=${1:-full}

    case $MODE in
        prereq)
            check_prerequisites
            ;;
        build)
            check_prerequisites
            build_project
            ;;
        test)
            check_prerequisites
            build_project
            run_unit_tests
            ;;
        docker)
            check_prerequisites
            start_docker_services
            test_api_health
            test_postgresql
            test_redis
            show_results
            ;;
        full)
            check_prerequisites
            build_project
            run_unit_tests
            start_docker_services
            test_api_health
            test_postgresql
            test_redis
            test_pipeline_creation
            test_pipeline_retrieval
            test_metrics
            show_results
            ;;
        cleanup)
            docker-compose down -v
            cleanup
            print_success "Cleanup complete"
            ;;
        *)
            echo "Usage: $0 {prereq|build|test|docker|full|cleanup}"
            echo ""
            echo "Modes:"
            echo "  prereq  - Check prerequisites only"
            echo "  build   - Build the project"
            echo "  test    - Run unit tests"
            echo "  docker  - Start and test Docker services"
            echo "  full    - Run all tests (default)"
            echo "  cleanup - Stop services and cleanup"
            exit 1
            ;;
    esac
}

# Trap errors
trap 'print_error "Test failed at line $LINENO"' ERR

# Run main function
main "$@"
