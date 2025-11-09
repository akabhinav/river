package com.river.connector.dest.streaming;

import com.river.core.connector.ConnectorException;
import com.river.core.connector.DestinationConnector;
import com.river.core.connector.ConnectorStats;
import com.river.core.connector.Connector.ConnectorType;
import com.river.core.context.DestinationContext;
import com.river.core.model.Record;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.concurrent.Future;
import java.util.List;
import java.util.ArrayList;

/**
 * Kafka destination connector for producing messages to Kafka topics
 */
@Slf4j
public class KafkaDestinationConnector implements DestinationConnector<KafkaDestinationConfig> {

    private KafkaDestinationConfig config;
    private DestinationContext context;
    private Producer<String, String> producer;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        log.info("Configuring Kafka destination connector");
        this.config = new KafkaDestinationConfig();

        config.setBootstrapServers((String) configMap.get("bootstrapServers"));
        config.setTopic((String) configMap.get("topic"));
        config.setKeyField((String) configMap.getOrDefault("keyField", "key"));
        config.setValueField((String) configMap.getOrDefault("valueField", "value"));

        config.validate();
        this.stats = ConnectorStats.builder().build();
    }

    @Override
    public void start(DestinationContext context) throws ConnectorException {
        log.info("Starting Kafka destination connector");
        this.context = context;

        try {
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.RETRIES_CONFIG, 3);
            props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
            props.put(ProducerConfig.LINGER_MS_CONFIG, 1);

            producer = new KafkaProducer<>(props);

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
    public void write(Stream<Record> records) throws ConnectorException {
        log.info("Writing data to Kafka");

        try {
            List<Future> futures = new ArrayList<>();

            records.forEach(record -> {
                String key = String.valueOf(record.getData().get(config.getKeyField()));
                String value = String.valueOf(record.getData().get(config.getValueField()));

                ProducerRecord<String, String> kafkaRecord = new ProducerRecord<>(
                        config.getTopic(),
                        key,
                        value
                );

                Future future = producer.send(kafkaRecord, (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Error sending message to Kafka", exception);
                        stats.incrementRecordsFailed(1);
                    } else {
                        stats.incrementRecordsProcessed(1);
                        log.debug("Message sent to partition {} offset {}",
                                metadata.partition(), metadata.offset());
                    }
                });

                futures.add(future);
            });

            // Wait for all sends to complete
            for (Future future : futures) {
                future.get();
            }

            producer.flush();

        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.DATA_ERROR,
                    "Failed to write to Kafka",
                    e
            );
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping Kafka destination connector");
        stats.complete();

        if (producer != null) {
            producer.close();
        }
    }

    @Override
    public String getName() {
        return "kafka-destination";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Kafka destination connector for producing messages to Kafka topics";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.DESTINATION;
    }

    @Override
    public Class<KafkaDestinationConfig> getConfigClass() {
        return KafkaDestinationConfig.class;
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }
}
