package com.river.connector.source.cache;

import com.river.core.connector.ConnectorConfig;
import com.river.core.connector.ConnectorException;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration for Redis source connector
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RedisSourceConfig extends ConnectorConfig {

    private String host;
    private Integer port = 6379;
    private String password;
    private Integer database = 0;
    private String pattern = "*";
    private Integer scanCount = 1000;

    @Override
    public void validate() throws ConnectorException {
        if (host == null || host.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Host is required"
            );
        }
    }
}
