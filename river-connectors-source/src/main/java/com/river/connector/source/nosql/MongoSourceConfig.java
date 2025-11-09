package com.river.connector.source.nosql;

import com.river.core.connector.ConnectorConfig;
import com.river.core.connector.ConnectorException;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration for MongoDB source connector
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MongoSourceConfig extends ConnectorConfig {

    private String uri;
    private String database;
    private String collection;
    private String filter; // MongoDB filter in JSON format
    private Integer batchSize = 1000;

    @Override
    public void validate() throws ConnectorException {
        if (uri == null || uri.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "MongoDB URI is required"
            );
        }
        if (database == null || database.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Database name is required"
            );
        }
        if (collection == null || collection.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Collection name is required"
            );
        }
    }
}
