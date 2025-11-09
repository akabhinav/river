package com.river.connector.source.jdbc;

import com.river.core.connector.ConnectorException;
import com.river.core.connector.SourceConnector;
import com.river.core.connector.ConnectorStats;
import com.river.core.connector.Connector.ConnectorType;
import com.river.core.context.SourceContext;
import com.river.core.model.Record;
import com.river.core.model.Schema;
import com.river.core.model.Field;
import com.river.core.model.DataType;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * MySQL source connector for extracting data from MySQL databases
 */
@Slf4j
public class MySQLSourceConnector implements SourceConnector<MySQLSourceConfig> {

    private MySQLSourceConfig config;
    private SourceContext context;
    private Connection connection;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        log.info("Configuring MySQL source connector");
        this.config = new MySQLSourceConfig();

        config.setHost((String) configMap.get("host"));
        config.setPort((Integer) configMap.getOrDefault("port", 3306));
        config.setDatabase((String) configMap.get("database"));
        config.setUsername((String) configMap.get("username"));
        config.setPassword((String) configMap.get("password"));
        config.setTable((String) configMap.get("table"));
        config.setQuery((String) configMap.get("query"));

        config.validate();
        this.stats = ConnectorStats.builder().build();
    }

    @Override
    public void start(SourceContext context) throws ConnectorException {
        log.info("Starting MySQL source connector");
        this.context = context;

        try {
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                    config.getHost(), config.getPort(), config.getDatabase());

            connection = DriverManager.getConnection(
                    jdbcUrl,
                    config.getUsername(),
                    config.getPassword()
            );

            log.info("Connected to MySQL: {}", jdbcUrl);
        } catch (SQLException e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONNECTION_ERROR,
                    "Failed to connect to MySQL",
                    e
            );
        }
    }

    @Override
    public Stream<Record> read() throws ConnectorException {
        log.info("Reading data from MySQL");

        try {
            String query = buildQuery();
            Statement stmt = connection.createStatement(
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY
            );
            stmt.setFetchSize(Integer.MIN_VALUE); // MySQL streaming mode

            ResultSet rs = stmt.executeQuery(query);
            Schema schema = extractSchema(rs.getMetaData());

            return StreamSupport.stream(
                    new Spliterators.AbstractSpliterator<Record>(
                            Long.MAX_VALUE,
                            Spliterator.ORDERED
                    ) {
                        @Override
                        public boolean tryAdvance(java.util.function.Consumer<? super Record> action) {
                            try {
                                if (rs.next()) {
                                    Record record = extractRecord(rs, schema);
                                    stats.incrementRecordsProcessed(1);
                                    action.accept(record);
                                    return true;
                                }
                                return false;
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    },
                    false
            ).onClose(() -> {
                try {
                    rs.close();
                    stmt.close();
                } catch (SQLException e) {
                    log.error("Error closing ResultSet", e);
                }
            });

        } catch (SQLException e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.DATA_ERROR,
                    "Failed to read from MySQL",
                    e
            );
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping MySQL source connector");
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
        return "mysql-source";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "MySQL source connector for extracting data from MySQL databases";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.SOURCE;
    }

    @Override
    public Class<MySQLSourceConfig> getConfigClass() {
        return MySQLSourceConfig.class;
    }

    @Override
    public boolean supportsIncremental() {
        return true;
    }

    @Override
    public boolean supportsCDC() {
        return true; // MySQL binlog
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }

    private String buildQuery() {
        if (config.getQuery() != null) {
            return config.getQuery();
        }
        return "SELECT * FROM " + config.getTable();
    }

    private Schema extractSchema(ResultSetMetaData metaData) throws SQLException {
        Schema schema = Schema.builder()
                .name(config.getTable())
                .build();

        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            int sqlType = metaData.getColumnType(i);
            DataType dataType = mapSqlTypeToDataType(sqlType);

            Field field = Field.builder()
                    .name(columnName)
                    .type(dataType)
                    .nullable(metaData.isNullable(i) == ResultSetMetaData.columnNullable)
                    .build();

            schema.addField(field);
        }

        return schema;
    }

    private Record extractRecord(ResultSet rs, Schema schema) throws SQLException {
        Map<String, Object> data = new HashMap<>();

        for (Field field : schema.getFields()) {
            Object value = rs.getObject(field.getName());
            data.put(field.getName(), value);
        }

        return Record.builder()
                .schema(schema)
                .data(data)
                .build();
    }

    private DataType mapSqlTypeToDataType(int sqlType) {
        return switch (sqlType) {
            case Types.BOOLEAN, Types.BIT -> DataType.BOOLEAN;
            case Types.TINYINT -> DataType.BYTE;
            case Types.SMALLINT -> DataType.SHORT;
            case Types.INTEGER -> DataType.INT;
            case Types.BIGINT -> DataType.LONG;
            case Types.REAL, Types.FLOAT -> DataType.FLOAT;
            case Types.DOUBLE -> DataType.DOUBLE;
            case Types.DECIMAL, Types.NUMERIC -> DataType.DECIMAL;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR -> DataType.STRING;
            case Types.DATE -> DataType.DATE;
            case Types.TIME -> DataType.TIME;
            case Types.TIMESTAMP -> DataType.TIMESTAMP;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> DataType.BINARY;
            default -> DataType.STRING;
        };
    }
}
