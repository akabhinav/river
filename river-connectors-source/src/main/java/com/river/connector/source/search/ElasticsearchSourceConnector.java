package com.river.connector.source.search;

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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Elasticsearch source connector for searching and reading documents
 */
@Slf4j
public class ElasticsearchSourceConnector implements SourceConnector<ElasticsearchSourceConfig> {

    private ElasticsearchSourceConfig config;
    private SourceContext context;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        log.info("Configuring Elasticsearch source connector");
        this.config = new ElasticsearchSourceConfig();

        config.setHost((String) configMap.get("host"));
        config.setPort((Integer) configMap.getOrDefault("port", 9200));
        config.setIndex((String) configMap.get("index"));
        config.setQuery((String) configMap.get("query"));
        config.setScrollSize((Integer) configMap.getOrDefault("scrollSize", 1000));
        config.setScrollTimeout((String) configMap.getOrDefault("scrollTimeout", "1m"));

        config.validate();
        this.stats = ConnectorStats.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void start(SourceContext context) throws ConnectorException {
        log.info("Starting Elasticsearch source connector");
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
    public Stream<Record> read() throws ConnectorException {
        log.info("Reading data from Elasticsearch");

        try {
            List<Record> allRecords = new ArrayList<>();
            Schema schema = createSchema();

            // Initial search with scroll
            String searchUrl = String.format("http://%s:%d/%s/_search?scroll=%s",
                    config.getHost(), config.getPort(), config.getIndex(), config.getScrollTimeout());

            String query = config.getQuery() != null ? config.getQuery() : "{\"query\":{\"match_all\":{}}}";
            String scrollQuery = String.format("{\"size\":%d,%s}",
                    config.getScrollSize(), query.substring(1));

            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(scrollQuery))
                    .build();

            HttpResponse<String> searchResponse = httpClient.send(searchRequest,
                    HttpResponse.BodyHandlers.ofString());

            JsonNode searchResult = objectMapper.readTree(searchResponse.body());
            String scrollId = searchResult.get("_scroll_id").asText();

            // Process initial batch
            JsonNode hits = searchResult.get("hits").get("hits");
            for (JsonNode hit : hits) {
                allRecords.add(convertToRecord(hit, schema));
                stats.incrementRecordsProcessed(1);
            }

            // Scroll through remaining results
            while (hits.size() > 0) {
                String scrollUrl = String.format("http://%s:%d/_search/scroll",
                        config.getHost(), config.getPort());

                String scrollBody = String.format("{\"scroll\":\"%s\",\"scroll_id\":\"%s\"}",
                        config.getScrollTimeout(), scrollId);

                HttpRequest scrollRequest = HttpRequest.newBuilder()
                        .uri(URI.create(scrollUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(scrollBody))
                        .build();

                HttpResponse<String> scrollResponse = httpClient.send(scrollRequest,
                        HttpResponse.BodyHandlers.ofString());

                JsonNode scrollResult = objectMapper.readTree(scrollResponse.body());
                hits = scrollResult.get("hits").get("hits");

                for (JsonNode hit : hits) {
                    allRecords.add(convertToRecord(hit, schema));
                    stats.incrementRecordsProcessed(1);
                }
            }

            return allRecords.stream();

        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.DATA_ERROR,
                    "Failed to read from Elasticsearch",
                    e
            );
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping Elasticsearch source connector");
        stats.complete();
    }

    @Override
    public String getName() {
        return "elasticsearch-source";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Elasticsearch source connector for searching and reading documents";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.SOURCE;
    }

    @Override
    public Class<ElasticsearchSourceConfig> getConfigClass() {
        return ElasticsearchSourceConfig.class;
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }

    private Schema createSchema() {
        Schema schema = Schema.builder()
                .name("elasticsearch_document")
                .build();

        schema.addField(Field.builder().name("_id").type(DataType.STRING).build());
        schema.addField(Field.builder().name("_index").type(DataType.STRING).build());
        schema.addField(Field.builder().name("_source").type(DataType.JSON).build());

        return schema;
    }

    private Record convertToRecord(JsonNode hit, Schema schema) {
        Map<String, Object> data = new HashMap<>();
        data.put("_id", hit.get("_id").asText());
        data.put("_index", hit.get("_index").asText());
        data.put("_source", hit.get("_source").toString());

        // Also add individual fields from _source
        JsonNode source = hit.get("_source");
        source.fields().forEachRemaining(entry -> {
            data.put(entry.getKey(), entry.getValue().asText());
        });

        return Record.builder()
                .schema(schema)
                .data(data)
                .build();
    }
}
