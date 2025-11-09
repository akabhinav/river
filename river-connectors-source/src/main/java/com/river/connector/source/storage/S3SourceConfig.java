package com.river.connector.source.storage;

import com.river.core.connector.ConnectorConfig;
import com.river.core.connector.ConnectorException;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration for S3 source connector
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class S3SourceConfig extends ConnectorConfig {

    private String bucket;
    private String prefix;
    private String region;
    private String accessKey;
    private String secretKey;
    private String format; // json, csv, parquet, avro

    @Override
    public void validate() throws ConnectorException {
        if (bucket == null || bucket.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Bucket is required"
            );
        }
        if (accessKey == null || accessKey.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Access key is required"
            );
        }
        if (secretKey == null || secretKey.isEmpty()) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONFIGURATION_ERROR,
                    "Secret key is required"
            );
        }
    }
}
