package com.river.connector.dest.nosql;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;
import com.river.core.connector.ConnectorException;
import com.river.core.connector.DestinationConnector;
import com.river.core.connector.ConnectorStats;
import com.river.core.connector.Connector.ConnectorType;
import com.river.core.context.DestinationContext;
import com.river.core.model.Record;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * MongoDB destination connector for writing documents to MongoDB collections
 */
@Slf4j
public class MongoDestinationConnector implements DestinationConnector<MongoDestinationConfig> {

    private MongoDestinationConfig config;
    private DestinationContext context;
    private MongoClient mongoClient;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        log.info("Configuring MongoDB destination connector");
        this.config = new MongoDestinationConfig();

        config.setUri((String) configMap.get("uri"));
        config.setDatabase((String) configMap.get("database"));
        config.setCollection((String) configMap.get("collection"));

        config.validate();
        this.stats = ConnectorStats.builder().build();
    }

    @Override
    public void start(DestinationContext context) throws ConnectorException {
        log.info("Starting MongoDB destination connector");
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
    public void write(Stream<Record> records) throws ConnectorException {
        log.info("Writing data to MongoDB");

        try {
            MongoDatabase database = mongoClient.getDatabase(config.getDatabase());
            MongoCollection<Document> collection = database.getCollection(config.getCollection());

            List<Document> batch = new ArrayList<>();
            int batchSize = context.getBatchSize();

            records.forEach(record -> {
                batch.add(convertToDocument(record));

                if (batch.size() >= batchSize) {
                    writeBatch(collection, new ArrayList<>(batch));
                    batch.clear();
                }
            });

            // Write remaining documents
            if (!batch.isEmpty()) {
                writeBatch(collection, batch);
            }

        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.DATA_ERROR,
                    "Failed to write to MongoDB",
                    e
            );
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping MongoDB destination connector");
        stats.complete();

        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public String getName() {
        return "mongodb-destination";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "MongoDB destination connector for writing documents to MongoDB collections";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.DESTINATION;
    }

    @Override
    public Class<MongoDestinationConfig> getConfigClass() {
        return MongoDestinationConfig.class;
    }

    @Override
    public WriteMode getWriteMode() {
        return WriteMode.APPEND;
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }

    private void writeBatch(MongoCollection<Document> collection, List<Document> batch) {
        try {
            collection.insertMany(batch, new InsertManyOptions().ordered(false));
            stats.incrementRecordsProcessed(batch.size());
            log.debug("Inserted {} documents", batch.size());
        } catch (Exception e) {
            log.error("Error inserting batch of {} documents", batch.size(), e);
            stats.incrementRecordsFailed(batch.size());
            throw e;
        }
    }

    private Document convertToDocument(Record record) {
        Document document = new Document();

        for (Map.Entry<String, Object> entry : record.getData().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Skip internal metadata fields
            if (!key.equals("document")) {
                document.append(key, value);
            }
        }

        return document;
    }
}
