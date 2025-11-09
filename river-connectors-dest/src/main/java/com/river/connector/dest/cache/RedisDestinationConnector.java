package com.river.connector.dest.cache;

import com.river.core.connector.ConnectorException;
import com.river.core.connector.DestinationConnector;
import com.river.core.connector.ConnectorStats;
import com.river.core.connector.Connector.ConnectorType;
import com.river.core.context.DestinationContext;
import com.river.core.model.Record;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.Map;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis destination connector for writing key-value pairs to Redis
 */
@Slf4j
public class RedisDestinationConnector implements DestinationConnector<RedisDestinationConfig> {

    private RedisDestinationConfig config;
    private DestinationContext context;
    private Jedis jedis;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        log.info("Configuring Redis destination connector");
        this.config = new RedisDestinationConfig();

        config.setHost((String) configMap.get("host"));
        config.setPort((Integer) configMap.getOrDefault("port", 6379));
        config.setPassword((String) configMap.get("password"));
        config.setDatabase((Integer) configMap.getOrDefault("database", 0));
        config.setKeyField((String) configMap.getOrDefault("keyField", "key"));
        config.setValueField((String) configMap.getOrDefault("valueField", "value"));
        config.setTtl((Integer) configMap.get("ttl"));

        config.validate();
        this.stats = ConnectorStats.builder().build();
    }

    @Override
    public void start(DestinationContext context) throws ConnectorException {
        log.info("Starting Redis destination connector");
        this.context = context;

        try {
            if (config.getPassword() != null && !config.getPassword().isEmpty()) {
                jedis = new Jedis(config.getHost(), config.getPort());
                jedis.auth(config.getPassword());
            } else {
                jedis = new Jedis(config.getHost(), config.getPort());
            }

            jedis.select(config.getDatabase());

            // Test connection
            jedis.ping();

            log.info("Connected to Redis: {}:{}/{}", config.getHost(), config.getPort(), config.getDatabase());
        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.CONNECTION_ERROR,
                    "Failed to connect to Redis",
                    e
            );
        }
    }

    @Override
    public void write(Stream<Record> records) throws ConnectorException {
        log.info("Writing data to Redis");

        try {
            List<Record> batch = new ArrayList<>();
            int batchSize = context.getBatchSize();

            records.forEach(record -> {
                batch.add(record);
                if (batch.size() >= batchSize) {
                    writeBatch(new ArrayList<>(batch));
                    batch.clear();
                }
            });

            // Write remaining records
            if (!batch.isEmpty()) {
                writeBatch(batch);
            }

        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.DATA_ERROR,
                    "Failed to write to Redis",
                    e
            );
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping Redis destination connector");
        stats.complete();

        if (jedis != null) {
            jedis.close();
        }
    }

    @Override
    public String getName() {
        return "redis-destination";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Redis destination connector for writing key-value pairs to Redis";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.DESTINATION;
    }

    @Override
    public Class<RedisDestinationConfig> getConfigClass() {
        return RedisDestinationConfig.class;
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }

    private void writeBatch(List<Record> batch) {
        Pipeline pipeline = jedis.pipelined();

        for (Record record : batch) {
            String key = String.valueOf(record.getData().get(config.getKeyField()));
            String value = String.valueOf(record.getData().get(config.getValueField()));

            if (key != null && value != null) {
                if (config.getTtl() != null && config.getTtl() > 0) {
                    pipeline.setex(key, config.getTtl(), value);
                } else {
                    pipeline.set(key, value);
                }
                stats.incrementRecordsProcessed(1);
            } else {
                stats.incrementRecordsFailed(1);
            }
        }

        pipeline.sync();
        log.debug("Wrote {} records to Redis", batch.size());
    }
}
