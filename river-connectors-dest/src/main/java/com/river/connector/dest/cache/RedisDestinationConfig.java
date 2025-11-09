package com.river.connector.dest.cache;

import com.river.core.connector.ConnectorConfig;
import com.river.core.connector.ConnectorException;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration for Redis destination connector
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RedisDestinationConfig extends ConnectorConfig {

    private String host;
    private Integer port = 6379;
    private String password;
    private Integer database = 0;
    private String keyField = "key";
    private String valueField = "value";
    private Integer ttl; // Time to live in seconds (optional)

    @Override
    public void validate() throws ConnectorException {
        if (host == null || host.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Host is required"
            );
        }
        if (keyField == null || keyField.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Key field is required"
            );
        }
        if (valueField == null || valueField.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Value field is required"
            );
        }
    }
}
