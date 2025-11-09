package com.river.connector.source.nosql;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * MongoDB source connector for reading documents from MongoDB collections
 */
@Slf4j
public class MongoSourceConnector implements SourceConnector<MongoSourceConfig> {

    private MongoSourceConfig config;
    private SourceContext context;
    private MongoClient mongoClient;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        log.info("Configuring MongoDB source connector");
        this.config = new MongoSourceConfig();

        config.setUri((String) configMap.get("uri"));
        config.setDatabase((String) configMap.get("database"));
        config.setCollection((String) configMap.get("collection"));
        config.setFilter((String) configMap.get("filter"));
        config.setBatchSize((Integer) configMap.getOrDefault("batchSize", 1000));

        config.validate();
        this.stats = ConnectorStats.builder().build();
    }

    @Override
    public void start(SourceContext context) throws ConnectorException {
        log.info("Starting MongoDB source connector");
        this.context = context;

        try {
            mongoClient = MongoClients.create(config.getUri());

            // Test connection
            mongoClient.getDatabase(config.getDatabase()).listCollectionNames().first();

            log.info("Connected to MongoDB: {}/{}", config.getDatabase(), config.getCollection());
        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONNECTION_ERROR,
                    "Failed to connect to MongoDB",
                    e
            );
        }
    }

    @Override
    public Stream<Record> read() throws ConnectorException {
        log.info("Reading data from MongoDB");

        try {
            MongoDatabase database = mongoClient.getDatabase(config.getDatabase());
            MongoCollection<Document> collection = database.getCollection(config.getCollection());

            // Apply filter if provided
            Iterable<Document> documents;
            if (config.getFilter() != null && !config.getFilter().isEmpty()) {
                Document filterDoc = Document.parse(config.getFilter());
                documents = collection.find(filterDoc).batchSize(config.getBatchSize());
            } else {
                documents = collection.find().batchSize(config.getBatchSize());
            }

            Schema schema = createSchema();

            return StreamSupport.stream(documents.spliterator(), false)
                    .map(doc -> convertToRecord(doc, schema))
                    .peek(record -> stats.incrementRecordsProcessed(1));

        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.DATA_ERROR,
                    "Failed to read from MongoDB",
                    e
            );
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping MongoDB source connector");
        stats.complete();

        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public String getName() {
        return "mongodb-source";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "MongoDB source connector for reading documents from MongoDB collections";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.SOURCE;
    }

    @Override
    public Class<MongoSourceConfig> getConfigClass() {
        return MongoSourceConfig.class;
    }

    @Override
    public boolean supportsIncremental() {
        return true;
    }

    @Override
    public boolean supportsCDC() {
        return true; // MongoDB Change Streams
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }

    private Schema createSchema() {
        Schema schema = Schema.builder()
                .name(config.getCollection())
                .build();

        // MongoDB documents are flexible, so we create a generic schema
        schema.addField(Field.builder().name("_id").type(DataType.STRING).build());
        schema.addField(Field.builder().name("document").type(DataType.JSON).build());

        return schema;
    }

    private Record convertToRecord(Document document, Schema schema) {
        Map<String, Object> data = new HashMap<>();

        // Extract _id
        data.put("_id", document.get("_id").toString());

        // Store entire document as JSON
        data.put("document", document.toJson());

        // Also add individual fields for easier access
        for (String key : document.keySet()) {
            if (!key.equals("_id")) {
                data.put(key, document.get(key));
            }
        }

        return Record.builder()
                .schema(schema)
                .data(data)
                .build();
    }
}
