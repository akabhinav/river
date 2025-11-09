package com.river.connector.source.cache;

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
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis source connector for reading keys and values from Redis
 */
@Slf4j
public class RedisSourceConnector implements SourceConnector<RedisSourceConfig> {

    private RedisSourceConfig config;
    private SourceContext context;
    private Jedis jedis;
    private ConnectorStats stats;

    @Override
    public void configure(Map<String, Object> configMap) throws ConnectorException {
        log.info("Configuring Redis source connector");
        this.config = new RedisSourceConfig();

        config.setHost((String) configMap.get("host"));
        config.setPort((Integer) configMap.getOrDefault("port", 6379));
        config.setPassword((String) configMap.get("password"));
        config.setDatabase((Integer) configMap.getOrDefault("database", 0));
        config.setPattern((String) configMap.getOrDefault("pattern", "*"));
        config.setScanCount((Integer) configMap.getOrDefault("scanCount", 1000));

        config.validate();
        this.stats = ConnectorStats.builder().build();
    }

    @Override
    public void start(SourceContext context) throws ConnectorException {
        log.info("Starting Redis source connector");
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
    public Stream<Record> read() throws ConnectorException {
        log.info("Reading data from Redis");

        try {
            List<String> allKeys = scanKeys();
            Schema schema = createSchema();

            return allKeys.stream()
                    .map(key -> readKey(key, schema))
                    .peek(record -> stats.incrementRecordsProcessed(1));

        } catch (Exception e) {
            throw new ConnectorException(
                    ConnectorException.ErrorCode.DATA_ERROR,
                    "Failed to read from Redis",
                    e
            );
        }
    }

    @Override
    public void stop() throws ConnectorException {
        log.info("Stopping Redis source connector");
        stats.complete();

        if (jedis != null) {
            jedis.close();
        }
    }

    @Override
    public String getName() {
        return "redis-source";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Redis source connector for reading keys and values from Redis";
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.SOURCE;
    }

    @Override
    public Class<RedisSourceConfig> getConfigClass() {
        return RedisSourceConfig.class;
    }

    @Override
    public ConnectorStats getStats() {
        return stats;
    }

    private List<String> scanKeys() {
        List<String> keys = new ArrayList<>();
        String cursor = "0";
        ScanParams scanParams = new ScanParams()
                .match(config.getPattern())
                .count(config.getScanCount());

        do {
            ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
            keys.addAll(scanResult.getResult());
            cursor = scanResult.getCursor();
        } while (!cursor.equals("0"));

        log.info("Found {} keys matching pattern: {}", keys.size(), config.getPattern());
        return keys;
    }

    private Schema createSchema() {
        Schema schema = Schema.builder()
                .name("redis_record")
                .build();

        schema.addField(Field.builder().name("key").type(DataType.STRING).build());
        schema.addField(Field.builder().name("value").type(DataType.STRING).build());
        schema.addField(Field.builder().name("type").type(DataType.STRING).build());
        schema.addField(Field.builder().name("ttl").type(DataType.LONG).build());

        return schema;
    }

    private Record readKey(String key, Schema schema) {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);

        // Get value based on type
        String type = jedis.type(key);
        data.put("type", type);

        switch (type) {
            case "string":
                data.put("value", jedis.get(key));
                break;
            case "list":
                data.put("value", String.join(",", jedis.lrange(key, 0, -1)));
                break;
            case "set":
                data.put("value", String.join(",", jedis.smembers(key)));
                break;
            case "zset":
                // Get sorted set with scores as a Map
                var zsetWithScores = jedis.zrangeWithScores(key, 0, -1);
                Map<String, Double> zsetMap = new HashMap<>();
                for (var tuple : zsetWithScores) {
                    zsetMap.put(tuple.getElement(), tuple.getScore());
                }
                data.put("value", zsetMap.toString());
                break;
            case "hash":
                data.put("value", jedis.hgetAll(key).toString());
                break;
            default:
                data.put("value", null);
        }

        // Get TTL
        Long ttl = jedis.ttl(key);
        data.put("ttl", ttl);

        return Record.builder()
                .schema(schema)
                .data(data)
                .build();
    }
}
