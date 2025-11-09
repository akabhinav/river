package com.river.connector.source.streaming;

import com.river.core.connector.ConnectorConfig;
import com.river.core.connector.ConnectorException;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration for Kafka source connector
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KafkaSourceConfig extends ConnectorConfig {

    private String bootstrapServers;
    private String topic;
    private String groupId;
    private String autoOffsetReset = "earliest";
    private Long pollTimeout = 1000L;

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
        if (groupId == null || groupId.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Group ID is required"
            );
        }
    }
}
