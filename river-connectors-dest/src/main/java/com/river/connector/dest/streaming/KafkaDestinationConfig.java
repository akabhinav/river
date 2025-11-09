package com.river.connector.dest.streaming;

import com.river.core.connector.ConnectorConfig;
import com.river.core.connector.ConnectorException;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration for Kafka destination connector
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KafkaDestinationConfig extends ConnectorConfig {

    private String bootstrapServers;
    private String topic;
    private String keyField = "key";
    private String valueField = "value";

    @Override
    public void validate() throws ConnectorException {
        if (bootstrapServers == null || bootstrapServers.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Bootstrap servers are required"
            );
        }
        if (topic == null || topic.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Topic is required"
            );
        }
    }
}
