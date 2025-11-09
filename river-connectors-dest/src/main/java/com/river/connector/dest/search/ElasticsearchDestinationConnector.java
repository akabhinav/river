package com.river.connector.dest.search;

import com.river.core.connector.ConnectorException;
import com.river.core.connector.DestinationConnector;
import com.river.core.connector.ConnectorStats;
import com.river.core.connector.Connector.ConnectorType;
import com.river.core.context.DestinationContext;
import com.river.core.model.Record;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Stream;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Elasticsearch destination connector for indexing documents
 */
@Slf4j
public class ElasticsearchDestinationConnector implements DestinationConnector<ElasticsearchDestinationConfig> {

    private ElasticsearchDestinationConfig config;
    private DestinationContext context;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        log.info("Configuring Elasticsearch destination connector");
        this.config = new ElasticsearchDestinationConfig();

        config.setHost((String) configMap.get("host"));
        config.setPort((Integer) configMap.getOrDefault("port", 9200));
        config.setIndex((String) configMap.get("index"));
        config.setIdField((String) configMap.get("idField"));

        config.validate();
        this.stats = ConnectorStats.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void start(DestinationContext context) throws ConnectorException {
        log.info("Starting Elasticsearch destination connector");
        this.context = context;

        try {
            httpClient = HttpClient.newBuilder().build();

            // Test connection
            String healthUrl = String.format("http://%s:%d/_cluster/health",
                    config.getHost(), config.getPort());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Connected to Elasticsearch: {}:{}", config.getHost(), config.getPort());
            } else {
                throw new Exception("Elasticsearch health check failed");
            }
        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONNECTION_ERROR,
                    "Failed to connect to Elasticsearch",
                    e
            );
        }
    }

    @Override
    public void write(Stream<Record> records) throws ConnectorException {
        log.info("Writing data to Elasticsearch");

        try {
            List<Record> batch = new ArrayList<>();
            int batchSize = context.getBatchSize();

            records.forEach(record -> {
                batch.add(record);
                if (batch.size() >= batchSize) {
                    try {
                        writeBatch(new ArrayList<>(batch));
                        batch.clear();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            // Write remaining records
            if (!batch.isEmpty()) {
                writeBatch(batch);
            }

        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.DATA_ERROR,
                    "Failed to write to Elasticsearch",
                    e
            );
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping Elasticsearch destination connector");
        stats.complete();
    }

    @Override
    public String getName() {
        return "elasticsearch-destination";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Elasticsearch destination connector for indexing documents";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.DESTINATION;
    }

    @Override
    public Class<ElasticsearchDestinationConfig> getConfigClass() {
        return ElasticsearchDestinationConfig.class;
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }

    private void writeBatch(List<Record> batch) throws Exception {
        // Use bulk API for better performance
        String bulkUrl = String.format("http://%s:%d/_bulk",
                config.getHost(), config.getPort());

        StringBuilder bulkBody = new StringBuilder();

        for (Record record : batch) {
            // Create action line
            String id = config.getIdField() != null ?
                    String.valueOf(record.getData().get(config.getIdField())) : null;

            if (id != null) {
                bulkBody.append(String.format("{\"index\":{\"_index\":\"%s\",\"_id\":\"%s\"}}\n",
                        config.getIndex(), id));
            } else {
                bulkBody.append(String.format("{\"index\":{\"_index\":\"%s\"}}\n",
                        config.getIndex()));
            }

            // Add document
            bulkBody.append(objectMapper.writeValueAsString(record.getData())).append("\n");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(bulkUrl))
                .header("Content-Type", "application/x-ndjson")
                .POST(HttpRequest.BodyPublishers.ofString(bulkBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            stats.incrementRecordsProcessed(batch.size());
            log.debug("Indexed {} documents", batch.size());
        } else {
            stats.incrementRecordsFailed(batch.size());
            log.error("Bulk index failed: {}", response.body());
        }
    }
}
