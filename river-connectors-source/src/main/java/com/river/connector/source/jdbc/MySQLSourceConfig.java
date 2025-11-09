package com.river.connector.source.jdbc;

import com.river.core.connector.ConnectorConfig;
import com.river.core.connector.ConnectorException;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration for MySQL source connector
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MySQLSourceConfig extends ConnectorConfig {

    private String host;
    private Integer port = 3306;
    private String database;
    private String username;
    private String password;
    private String table;
    private String query;

    @Override
    public void validate() throws ConnectorException {
        if (host == null || host.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Host is required"
            );
        }
        if (database == null || database.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Database is required"
            );
        }
        if (username == null || username.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Username is required"
            );
        }
        if (table == null && query == null) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Either table or query must be specified"
            );
        }
    }
}
