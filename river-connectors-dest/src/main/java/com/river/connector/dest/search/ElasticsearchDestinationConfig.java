package com.river.connector.dest.search;

import com.river.core.connector.ConnectorConfig;
import com.river.core.connector.ConnectorException;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration for Elasticsearch destination connector
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ElasticsearchDestinationConfig extends ConnectorConfig {

    private String host;
    private Integer port = 9200;
    private String index;
    private String idField; // Optional: field to use as document ID

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
