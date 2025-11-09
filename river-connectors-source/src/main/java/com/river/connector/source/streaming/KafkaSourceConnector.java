package com.river.connector.source.streaming;

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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Kafka source connector for consuming messages from Kafka topics
 */
@Slf4j
public class KafkaSourceConnector implements SourceConnector<KafkaSourceConfig> {

    private KafkaSourceConfig config;
    private SourceContext context;
    private Consumer<String, String> consumer;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        log.info("Configuring Kafka source connector");
        this.config = new KafkaSourceConfig();

        config.setBootstrapServers((String) configMap.get("bootstrapServers"));
        config.setTopic((String) configMap.get("topic"));
        config.setGroupId((String) configMap.get("groupId"));
        config.setAutoOffsetReset((String) configMap.getOrDefault("autoOffsetReset", "earliest"));
        config.setPollTimeout((Long) configMap.getOrDefault("pollTimeout", 1000L));

        config.validate();
        this.stats = ConnectorStats.builder().build();
    }

    @Override
    public void start(SourceContext context) throws ConnectorException {
        log.info("Starting Kafka source connector");
        this.context = context;

        try {
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getGroupId());
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.getAutoOffsetReset());
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
            props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(config.getTopic()));

            log.info("Connected to Kafka: {}, topic: {}", config.getBootstrapServers(), config.getTopic());
        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONNECTION_ERROR,
                    "Failed to connect to Kafka",
                    e
            );
        }
    }

    @Override
    public Stream<Record> read() throws ConnectorException {
        log.info("Reading data from Kafka");

        try {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(config.getPollTimeout()));

            Schema schema = createSchema();

            return StreamSupport.stream(records.spliterator(), false)
                    .map(kafkaRecord -> convertToRecord(kafkaRecord, schema))
                    .peek(record -> stats.incrementRecordsProcessed(1));

        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.DATA_ERROR,
                    "Failed to read from Kafka",
                    e
            );
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping Kafka source connector");
        stats.complete();

        if (consumer != null) {
            consumer.close();
        }
    }

    @Override
    public String getName() {
        return "kafka-source";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Kafka source connector for consuming messages from Kafka topics";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.SOURCE;
    }

    @Override
    public Class<KafkaSourceConfig> getConfigClass() {
        return KafkaSourceConfig.class;
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }

    private Schema createSchema() {
        Schema schema = Schema.builder()
                .name("kafka_record")
                .build();

        schema.addField(Field.builder().name("key").type(DataType.STRING).build());
        schema.addField(Field.builder().name("value").type(DataType.STRING).build());
        schema.addField(Field.builder().name("topic").type(DataType.STRING).build());
        schema.addField(Field.builder().name("partition").type(DataType.INT).build());
        schema.addField(Field.builder().name("offset").type(DataType.LONG).build());
        schema.addField(Field.builder().name("timestamp").type(DataType.TIMESTAMP).build());

        return schema;
    }

    private Record convertToRecord(ConsumerRecord<String, String> kafkaRecord, Schema schema) {
        Map<String, Object> data = new HashMap<>();
        data.put("key", kafkaRecord.key());
        data.put("value", kafkaRecord.value());
        data.put("topic", kafkaRecord.topic());
        data.put("partition", kafkaRecord.partition());
        data.put("offset", kafkaRecord.offset());
        data.put("timestamp", kafkaRecord.timestamp());

        return Record.builder()
                .schema(schema)
                .data(data)
                .build();
    }
}
