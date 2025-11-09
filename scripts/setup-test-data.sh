#!/bin/bash

# River Platform - Test Data Setup Script
# This script creates test data in all data stores for connector testing

set -e

echo "=========================================="
echo "River Platform - Test Data Setup"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Setting up test data in all data stores...${NC}"
echo ""

# 1. PostgreSQL Test Data
echo "1. Setting up PostgreSQL test data..."
docker exec -i river-postgres psql -U river -d river_metadata << 'EOF'
-- Create test tables
DROP TABLE IF EXISTS test_users CASCADE;
CREATE TABLE test_users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW()
);

DROP TABLE IF EXISTS test_orders CASCADE;
CREATE TABLE test_orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES test_users(id),
    product_name VARCHAR(100),
    amount DECIMAL(10,2),
    order_date TIMESTAMP DEFAULT NOW()
);

-- Insert test data
INSERT INTO test_users (name, email, status) VALUES
    ('Alice Johnson', 'alice@example.com', 'active'),
    ('Bob Smith', 'bob@example.com', 'active'),
    ('Charlie Brown', 'charlie@example.com', 'inactive'),
    ('Diana Prince', 'diana@example.com', 'active'),
    ('Eve Wilson', 'eve@example.com', 'active');

INSERT INTO test_orders (user_id, product_name, amount) VALUES
    (1, 'Laptop', 1299.99),
    (1, 'Mouse', 29.99),
    (2, 'Keyboard', 89.99),
    (3, 'Monitor', 399.99),
    (4, 'Headphones', 149.99);

-- Display counts
SELECT 'PostgreSQL setup complete' as status;
SELECT COUNT(*) as users_count FROM test_users;
SELECT COUNT(*) as orders_count FROM test_orders;
EOF
echo -e "${GREEN}✓ PostgreSQL data created${NC}"
echo ""

# 2. MySQL Test Data
echo "2. Setting up MySQL test data..."
docker exec -i river-mysql mysql -uriver -priver_pass << 'EOF'
CREATE DATABASE IF NOT EXISTS river_test;
USE river_test;

-- Create test tables
DROP TABLE IF EXISTS products;
CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    category VARCHAR(50),
    price DECIMAL(10,2),
    stock INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS log_summary;
CREATE TABLE log_summary (
    id INT AUTO_INCREMENT PRIMARY KEY,
    level VARCHAR(20),
    message TEXT,
    timestamp DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert test data
INSERT INTO products (name, category, price, stock) VALUES
    ('Laptop Pro', 'Electronics', 1999.99, 50),
    ('Wireless Mouse', 'Accessories', 29.99, 200),
    ('USB-C Cable', 'Accessories', 19.99, 500),
    ('External SSD 1TB', 'Storage', 149.99, 75),
    ('Mechanical Keyboard', 'Accessories', 129.99, 100),
    ('4K Monitor', 'Electronics', 599.99, 30),
    ('Webcam HD', 'Electronics', 79.99, 150),
    ('Desk Lamp', 'Furniture', 39.99, 80);

-- Display counts
SELECT 'MySQL setup complete' as status;
SELECT COUNT(*) as products_count FROM products;
EOF
echo -e "${GREEN}✓ MySQL data created${NC}"
echo ""

# 3. MongoDB Test Data
echo "3. Setting up MongoDB test data..."
docker exec -i river-mongodb mongosh -u river -p river_pass << 'EOF'
use river_test;

// Drop existing collections
db.customers.drop();
db.events.drop();
db.users_from_postgres.drop();

// Create customers collection
db.customers.insertMany([
    {
        name: "John Doe",
        email: "john@example.com",
        status: "active",
        age: 32,
        address: { city: "New York", country: "USA" },
        tags: ["premium", "verified"],
        created_at: new Date()
    },
    {
        name: "Jane Smith",
        email: "jane@example.com",
        status: "active",
        age: 28,
        address: { city: "London", country: "UK" },
        tags: ["verified"],
        created_at: new Date()
    },
    {
        name: "Bob Wilson",
        email: "bob@example.com",
        status: "inactive",
        age: 45,
        address: { city: "Toronto", country: "Canada" },
        tags: [],
        created_at: new Date()
    },
    {
        name: "Alice Cooper",
        email: "alice@example.com",
        status: "active",
        age: 35,
        address: { city: "Sydney", country: "Australia" },
        tags: ["premium"],
        created_at: new Date()
    }
]);

// Create events collection for testing
db.events.insertMany([
    {
        event_type: "login",
        user_id: "user123",
        timestamp: new Date(),
        metadata: { ip: "192.168.1.1", device: "Chrome" }
    },
    {
        event_type: "purchase",
        user_id: "user456",
        amount: 99.99,
        timestamp: new Date(),
        metadata: { product_id: "prod789" }
    }
]);

// Display counts
print("MongoDB setup complete");
print("Customers:", db.customers.countDocuments({}));
print("Events:", db.events.countDocuments({}));
EOF
echo -e "${GREEN}✓ MongoDB data created${NC}"
echo ""

# 4. Redis Test Data
echo "4. Setting up Redis test data..."
docker exec -i river-redis redis-cli << 'EOF'
FLUSHDB
SET user:1 "Alice Johnson"
SET user:2 "Bob Smith"
SET user:3 "Charlie Brown"
SET user:4 "Diana Prince"
SET user:5 "Eve Wilson"

HSET user:1:profile name "Alice Johnson" email "alice@example.com" status "active"
HSET user:2:profile name "Bob Smith" email "bob@example.com" status "active"
HSET user:3:profile name "Charlie Brown" email "charlie@example.com" status "inactive"

LPUSH recent_orders "order:1001" "order:1002" "order:1003"

SADD active_users "user:1" "user:2" "user:4" "user:5"

ZADD leaderboard 100 "user:1" 85 "user:2" 92 "user:4" 78 "user:5"

SETEX session:abc123 3600 "user:1"
SETEX session:def456 3600 "user:2"

KEYS *
DBSIZE
EOF
echo -e "${GREEN}✓ Redis data created${NC}"
echo ""

# 5. Kafka Test Topics and Data
echo "5. Setting up Kafka topics and test data..."

# Create topics
docker exec river-kafka kafka-topics \
    --bootstrap-server localhost:29092 \
    --create \
    --if-not-exists \
    --topic user-events \
    --partitions 3 \
    --replication-factor 1 \
    > /dev/null 2>&1

docker exec river-kafka kafka-topics \
    --bootstrap-server localhost:29092 \
    --create \
    --if-not-exists \
    --topic mysql-changes \
    --partitions 3 \
    --replication-factor 1 \
    > /dev/null 2>&1

docker exec river-kafka kafka-topics \
    --bootstrap-server localhost:29092 \
    --create \
    --if-not-exists \
    --topic test-events \
    --partitions 3 \
    --replication-factor 1 \
    > /dev/null 2>&1

# Produce test messages
echo '{"event":"login","user":"alice","timestamp":"'$(date -Iseconds)'"}' | \
    docker exec -i river-kafka kafka-console-producer \
    --bootstrap-server localhost:29092 \
    --topic user-events \
    > /dev/null 2>&1

echo '{"event":"purchase","user":"bob","amount":99.99,"timestamp":"'$(date -Iseconds)'"}' | \
    docker exec -i river-kafka kafka-console-producer \
    --bootstrap-server localhost:29092 \
    --topic user-events \
    > /dev/null 2>&1

echo '{"event":"logout","user":"alice","timestamp":"'$(date -Iseconds)'"}' | \
    docker exec -i river-kafka kafka-console-producer \
    --bootstrap-server localhost:29092 \
    --topic user-events \
    > /dev/null 2>&1

# List topics
echo "Created topics:"
docker exec river-kafka kafka-topics \
    --bootstrap-server localhost:29092 \
    --list

echo -e "${GREEN}✓ Kafka topics and data created${NC}"
echo ""

# 6. Elasticsearch Test Data
echo "6. Setting up Elasticsearch test data..."

# Create index with mapping
curl -s -X PUT "http://localhost:9200/test-logs" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "properties": {
      "level": { "type": "keyword" },
      "message": { "type": "text" },
      "service": { "type": "keyword" },
      "@timestamp": { "type": "date" }
    }
  }
}' > /dev/null

# Bulk insert test documents
curl -s -X POST "http://localhost:9200/test-logs/_bulk" -H 'Content-Type: application/json' -d'
{"index":{}}
{"level":"INFO","message":"Application started successfully","service":"api","@timestamp":"'$(date -Iseconds)'"}
{"index":{}}
{"level":"ERROR","message":"Database connection failed","service":"api","@timestamp":"'$(date -Iseconds)'"}
{"index":{}}
{"level":"WARN","message":"High memory usage detected","service":"worker","@timestamp":"'$(date -Iseconds)'"}
{"index":{}}
{"level":"INFO","message":"User authentication successful","service":"auth","@timestamp":"'$(date -Iseconds)'"}
{"index":{}}
{"level":"ERROR","message":"Payment processing failed","service":"payment","@timestamp":"'$(date -Iseconds)'"}
{"index":{}}
{"level":"DEBUG","message":"Cache hit for user profile","service":"cache","@timestamp":"'$(date -Iseconds)'"}
' > /dev/null

# Create customers index
curl -s -X PUT "http://localhost:9200/customers" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "properties": {
      "name": { "type": "text" },
      "email": { "type": "keyword" },
      "status": { "type": "keyword" }
    }
  }
}' > /dev/null

# Wait for indexing
sleep 2

# Get document counts
LOG_COUNT=$(curl -s "http://localhost:9200/test-logs/_count" | grep -o '"count":[0-9]*' | cut -d: -f2)
echo "Indexed documents: $LOG_COUNT"

echo -e "${GREEN}✓ Elasticsearch data created${NC}"
echo ""

# Summary
echo "=========================================="
echo -e "${GREEN}✓ All test data setup complete!${NC}"
echo "=========================================="
echo ""
echo "Data Summary:"
echo "-------------"
echo "PostgreSQL:    5 users, 5 orders in 'river_metadata'"
echo "MySQL:         8 products in 'river_test'"
echo "MongoDB:       4 customers, 2 events in 'river_test'"
echo "Redis:         15+ keys (strings, hashes, lists, sets, sorted sets)"
echo "Kafka:         3 topics with sample messages"
echo "Elasticsearch: 6 log documents in 'test-logs' index"
echo ""
echo "Next steps:"
echo "1. Run './scripts/verify-services.sh' to verify all services"
echo "2. Run './scripts/test-connectors.sh' to test individual connectors"
echo "3. Run './scripts/test-pipelines.sh' to test example pipelines"
echo ""
