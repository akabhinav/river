package com.river.connector.source.search;

import com.river.core.connector.ConnectorConfig;
import com.river.core.connector.ConnectorException;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration for Elasticsearch source connector
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ElasticsearchSourceConfig extends ConnectorConfig {

    private String host;
    private Integer port = 9200;
    private String index;
    private String query; // Elasticsearch query DSL in JSON format
    private Integer scrollSize = 1000;
    private String scrollTimeout = "1m";

    @Override
    public void validate() throws ConnectorException {
        if (host == null || host.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Host is required"
            );
        }
        if (index == null || index.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Index is required"
            );
        }
    }
}
