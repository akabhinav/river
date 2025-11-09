package com.river.connector.source.storage;

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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Amazon S3 source connector
 */
@Slf4j
public class S3SourceConnector implements SourceConnector<S3SourceConfig> {

    private S3SourceConfig config;
    private SourceContext context;
    private S3Client s3Client;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        log.info("Configuring S3 source connector");
        this.config = new S3SourceConfig();

        config.setBucket((String) configMap.get("bucket"));
        config.setPrefix((String) configMap.get("prefix"));
        config.setRegion((String) configMap.getOrDefault("region", "us-east-1"));
        config.setAccessKey((String) configMap.get("accessKey"));
        config.setSecretKey((String) configMap.get("secretKey"));
        config.setFormat((String) configMap.getOrDefault("format", "json"));

        config.validate();
        this.stats = ConnectorStats.builder().build();
    }

    @Override
    public void start(SourceContext context) throws ConnectorException {
        log.info("Starting S3 source connector");
        this.context = context;

        try {
            var credentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())
            );

            s3Client = S3Client.builder()
                    .region(Region.of(config.getRegion()))
                    .credentialsProvider(credentialsProvider)
                    .build();

            log.info("Connected to S3 bucket: {}", config.getBucket());
        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONNECTION_ERROR,
                    "Failed to connect to S3",
                    e
            );
        }
    }

    @Override
    public Stream<Record> read() throws ConnectorException {
        log.info("Reading data from S3");

        try {
            List<S3Object> objects = listObjects();
            return objects.stream()
                    .flatMap(this::readObject);
        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.DATA_ERROR,
                    "Failed to read from S3",
                    e
            );
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping S3 source connector");
        stats.complete();

        if (s3Client != null) {
            s3Client.close();
        }
    }

    @Override
    public String getName() {
        return "s3-source";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Amazon S3 source connector for reading files from S3";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.SOURCE;
    }

    @Override
    public Class<S3SourceConfig> getConfigClass() {
        return S3SourceConfig.class;
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }

    private List<S3Object> listObjects() {
        List<S3Object> allObjects = new ArrayList<>();

        ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(config.getBucket());

        if (config.getPrefix() != null) {
            requestBuilder.prefix(config.getPrefix());
        }

        ListObjectsV2Request request = requestBuilder.build();
        ListObjectsV2Response response;

        do {
            response = s3Client.listObjectsV2(request);
            allObjects.addAll(response.contents());

            request = request.toBuilder()
                    .continuationToken(response.nextContinuationToken())
                    .build();
        } while (response.isTruncated());

        log.info("Found {} objects in S3", allObjects.size());
        return allObjects;
    }

    private Stream<Record> readObject(S3Object s3Object) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(config.getBucket())
                    .key(s3Object.key())
                    .build();

            var objectResponse = s3Client.getObject(getObjectRequest);

            // For simplicity, treating each line as a record (CSV/JSON lines)
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(objectResponse)
            );

            Schema schema = createSimpleSchema();

            return reader.lines()
                    .map(line -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("key", s3Object.key());
                        data.put("content", line);
                        data.put("size", s3Object.size());
                        data.put("lastModified", s3Object.lastModified().toString());

                        stats.incrementRecordsProcessed(1);
                        stats.incrementBytesProcessed(line.length());

                        return Record.builder()
                                .schema(schema)
                                .data(data)
                                .build();
                    });

        } catch (Exception e) {
            log.error("Error reading S3 object: {}", s3Object.key(), e);
            return Stream.empty();
        }
    }

    private Schema createSimpleSchema() {
        Schema schema = Schema.builder()
                .name("s3_record")
                .build();

        schema.addField(Field.builder().name("key").type(DataType.STRING).build());
        schema.addField(Field.builder().name("content").type(DataType.STRING).build());
        schema.addField(Field.builder().name("size").type(DataType.LONG).build());
        schema.addField(Field.builder().name("lastModified").type(DataType.STRING).build());

        return schema;
    }
}
