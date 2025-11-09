# Contributing to River Platform

Thank you for your interest in contributing to River Platform!

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/river.git`
3. Create a feature branch: `git checkout -b feature/my-feature`
4. Make your changes
5. Test your changes: `mvn clean test`
6. Commit your changes: `git commit -am 'Add new feature'`
7. Push to your fork: `git push origin feature/my-feature`
8. Create a Pull Request

## Development Setup

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker (for integration tests)
- IDE (IntelliJ IDEA or Eclipse recommended)

### Build the Project

```bash
mvn clean install
```

### Run Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Specific module
mvn test -pl river-core
```

### Code Style

- Follow Java code conventions
- Use meaningful variable and method names
- Add Javadoc for public APIs
- Keep methods small and focused
- Write tests for new functionality

### Commit Messages

Use conventional commit format:

```
feat: add MongoDB source connector
fix: resolve connection timeout in PostgreSQL connector
docs: update connector development guide
test: add integration tests for S3 connector
refactor: simplify pipeline executor logic
```

## Adding a New Connector

1. Create connector class implementing `SourceConnector` or `DestinationConnector`
2. Create configuration class extending `ConnectorConfig`
3. Add unit tests
4. Add integration tests (if applicable)
5. Update documentation
6. Add example pipeline

See [Connector Development Guide](docs/connector-development.md) for details.

## Testing

### Unit Tests

```java
@Test
void testConnectorConfiguration() {
    // Test configuration parsing
}
```

### Integration Tests

Use TestContainers:

```java
@Testcontainers
class PostgresConnectorTest {
    @Container
    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
}
```

## Documentation

- Update README.md for major features
- Add/update docs in `docs/` directory
- Include examples in `examples/` directory
- Add Javadoc for public APIs

## Pull Request Process

1. Update documentation
2. Add tests for new functionality
3. Ensure all tests pass
4. Update CHANGELOG.md
5. Request review from maintainers

## Code Review

- Address review comments promptly
- Keep PRs focused and reasonably sized
- Squash commits before merging

## Questions?

- Open an issue for discussion
- Join our community chat
- Email: dev@river-platform.io

Thank you for contributing!
