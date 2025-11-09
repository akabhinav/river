#!/bin/bash

# Script to create test data for local testing

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

echo "Creating test data for River Platform..."
echo ""

# Wait for PostgreSQL to be ready
print_info "Waiting for PostgreSQL to be ready..."
until docker exec river-postgres-1 pg_isready -U river > /dev/null 2>&1; do
    echo -n "."
    sleep 1
done
echo ""
print_success "PostgreSQL is ready"

# Create source database and table
print_info "Creating source database and table..."
docker exec -i river-postgres-1 psql -U river -d river_metadata <<EOF
-- Create source schema
CREATE SCHEMA IF NOT EXISTS test_source;

-- Create source table with sample data
DROP TABLE IF EXISTS test_source.users CASCADE;
CREATE TABLE test_source.users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(100),
    age INTEGER,
    country VARCHAR(50),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Insert sample data
INSERT INTO test_source.users (username, email, full_name, age, country, status) VALUES
    ('alice', 'alice@example.com', 'Alice Johnson', 28, 'USA', 'active'),
    ('bob', 'bob@example.com', 'Bob Smith', 35, 'UK', 'active'),
    ('charlie', 'charlie@example.com', 'Charlie Brown', 42, 'Canada', 'active'),
    ('diana', 'diana@example.com', 'Diana Prince', 30, 'USA', 'active'),
    ('edward', 'edward@example.com', 'Edward Norton', 45, 'Australia', 'inactive'),
    ('fiona', 'fiona@example.com', 'Fiona Apple', 33, 'UK', 'active'),
    ('george', 'george@example.com', 'George Martin', 50, 'USA', 'active'),
    ('hannah', 'hannah@example.com', 'Hannah Montana', 25, 'Canada', 'active'),
    ('ivan', 'ivan@example.com', 'Ivan Petrov', 38, 'Russia', 'active'),
    ('julia', 'julia@example.com', 'Julia Roberts', 29, 'USA', 'active');

-- Create additional records for testing pagination
INSERT INTO test_source.users (username, email, full_name, age, country)
SELECT
    'user_' || generate_series,
    'user_' || generate_series || '@example.com',
    'User ' || generate_series,
    20 + (random() * 50)::integer,
    CASE (random() * 4)::integer
        WHEN 0 THEN 'USA'
        WHEN 1 THEN 'UK'
        WHEN 2 THEN 'Canada'
        WHEN 3 THEN 'Australia'
        ELSE 'Other'
    END
FROM generate_series(1, 100);

-- Create orders table for join testing
DROP TABLE IF EXISTS test_source.orders CASCADE;
CREATE TABLE test_source.orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES test_source.users(id),
    product VARCHAR(100),
    amount DECIMAL(10, 2),
    order_date TIMESTAMP DEFAULT NOW(),
    status VARCHAR(20) DEFAULT 'pending'
);

-- Insert sample orders
INSERT INTO test_source.orders (user_id, product, amount, status)
SELECT
    (random() * 100 + 1)::integer,
    'Product ' || (random() * 50 + 1)::integer,
    (random() * 500 + 10)::numeric(10,2),
    CASE (random() * 3)::integer
        WHEN 0 THEN 'pending'
        WHEN 1 THEN 'completed'
        WHEN 2 THEN 'cancelled'
        ELSE 'shipped'
    END
FROM generate_series(1, 500);

-- Grant permissions
GRANT USAGE ON SCHEMA test_source TO river;
GRANT SELECT ON ALL TABLES IN SCHEMA test_source TO river;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA test_source TO river;

EOF

print_success "Source data created"

# Create destination schema
print_info "Creating destination schema..."
docker exec -i river-postgres-1 psql -U river -d river_metadata <<EOF
-- Create destination schema
CREATE SCHEMA IF NOT EXISTS test_destination;

-- Create destination tables (empty, ready for data loading)
DROP TABLE IF EXISTS test_destination.users CASCADE;
CREATE TABLE test_destination.users (
    id INTEGER,
    username VARCHAR(50),
    email VARCHAR(100),
    full_name VARCHAR(100),
    age INTEGER,
    country VARCHAR(50),
    status VARCHAR(20),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    loaded_at TIMESTAMP DEFAULT NOW()
);

DROP TABLE IF EXISTS test_destination.orders CASCADE;
CREATE TABLE test_destination.orders (
    id INTEGER,
    user_id INTEGER,
    product VARCHAR(100),
    amount DECIMAL(10, 2),
    order_date TIMESTAMP,
    status VARCHAR(20),
    loaded_at TIMESTAMP DEFAULT NOW()
);

-- Grant permissions
GRANT USAGE ON SCHEMA test_destination TO river;
GRANT ALL ON ALL TABLES IN SCHEMA test_destination TO river;

EOF

print_success "Destination schema created"

# Show statistics
print_info "Test data statistics:"
docker exec -i river-postgres-1 psql -U river -d river_metadata <<EOF
SELECT
    'Users' as table_name,
    COUNT(*) as record_count
FROM test_source.users
UNION ALL
SELECT
    'Orders' as table_name,
    COUNT(*) as record_count
FROM test_source.orders;
EOF

echo ""
print_success "Test data creation complete!"
echo ""
echo "Available test tables:"
echo "  Source:"
echo "    • test_source.users ($(docker exec river-postgres-1 psql -U river -d river_metadata -t -c 'SELECT COUNT(*) FROM test_source.users') rows)"
echo "    • test_source.orders ($(docker exec river-postgres-1 psql -U river -d river_metadata -t -c 'SELECT COUNT(*) FROM test_source.orders') rows)"
echo ""
echo "  Destination:"
echo "    • test_destination.users (empty)"
echo "    • test_destination.orders (empty)"
echo ""
echo "To query the data:"
echo "  docker exec -it river-postgres-1 psql -U river -d river_metadata"
echo "  \\dt test_source.*"
echo "  SELECT * FROM test_source.users LIMIT 10;"
echo ""
