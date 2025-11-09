package com.river.connector.dest.jdbc;

import com.river.core.connector.ConnectorException;
import com.river.core.connector.DestinationConnector;
import com.river.core.connector.ConnectorStats;
import com.river.core.connector.Connector.ConnectorType;
import com.river.core.context.DestinationContext;
import com.river.core.model.Record;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * PostgreSQL destination connector
 */
@Slf4j
public class PostgresDestinationConnector implements DestinationConnector<PostgresDestinationConfig> {

    private PostgresDestinationConfig config;
    private DestinationContext context;
    private Connection connection;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        log.info("Configuring PostgreSQL destination connector");
        this.config = new PostgresDestinationConfig();

        config.setHost((String) configMap.get("host"));
        config.setPort((Integer) configMap.getOrDefault("port", 5432));
        config.setDatabase((String) configMap.get("database"));
        config.setUsername((String) configMap.get("username"));
        config.setPassword((String) configMap.get("password"));
        config.setTable((String) configMap.get("table"));

        config.validate();
        this.stats = ConnectorStats.builder().build();
    }

    @Override
    public void start(DestinationContext context) throws ConnectorException {
        log.info("Starting PostgreSQL destination connector");
        this.context = context;

        try {
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s",
                    config.getHost(), config.getPort(), config.getDatabase());

            connection = DriverManager.getConnection(
                    jdbcUrl,
                    config.getUsername(),
                    config.getPassword()
            );

            connection.setAutoCommit(!context.getTransactional());

            log.info("Connected to PostgreSQL: {}", jdbcUrl);
        } catch (SQLException e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONNECTION_ERROR,
                    "Failed to connect to PostgreSQL",
                    e
            );
        }
    }

    @Override
    public void write(Stream<Record> records) throws ConnectorException {
        log.info("Writing data to PostgreSQL");

        try {
            List<Record> batch = new ArrayList<>();
            int batchSize = context.getBatchSize();

            records.forEach(record -> {
                batch.add(record);
                if (batch.size() >= batchSize) {
                    try {
                        writeBatch(new ArrayList<>(batch));
                        batch.clear();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            // Write remaining records
            if (!batch.isEmpty()) {
                writeBatch(batch);
            }

            if (context.getTransactional()) {
                connection.commit();
            }

        } catch (Exception e) {
            if (context.getTransactional()) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    log.error("Error rolling back transaction", ex);
                }
            }
            throw new ConnectorException(
                    ConnectorException.ErrorCode.DATA_ERROR,
                    "Failed to write to PostgreSQL",
                    e
            );
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping PostgreSQL destination connector");
        stats.complete();

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("Error closing connection", e);
            }
        }
    }

    @Override
    public String getName() {
        return "postgresql-destination";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "PostgreSQL destination connector for loading data into PostgreSQL databases";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.DESTINATION;
    }

    @Override
    public Class<PostgresDestinationConfig> getConfigClass() {
        return PostgresDestinationConfig.class;
    }

    @Override
    public boolean supportsTransactions() {
        return true;
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }

    private void writeBatch(List<Record> batch) throws SQLException {
        if (batch.isEmpty()) {
            return;
        }

        Record firstRecord = batch.get(0);
        String insertSql = buildInsertStatement(firstRecord);

        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            for (Record record : batch) {
                setParameters(pstmt, record);
                pstmt.addBatch();
                stats.incrementRecordsProcessed(1);
            }

            pstmt.executeBatch();
        }
    }

    private String buildInsertStatement(Record record) {
        List<String> columns = new ArrayList<>(record.getData().keySet());
        String columnList = String.join(", ", columns);
        String valuePlaceholders = String.join(", ",
                columns.stream().map(c -> "?").toList());

        return String.format("INSERT INTO %s (%s) VALUES (%s)",
                config.getTable(), columnList, valuePlaceholders);
    }

    private void setParameters(PreparedStatement pstmt, Record record) throws SQLException {
        int index = 1;
        for (Object value : record.getData().values()) {
            pstmt.setObject(index++, value);
        }
    }
}
