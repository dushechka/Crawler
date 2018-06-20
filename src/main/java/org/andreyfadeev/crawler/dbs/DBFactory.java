package org.andreyfadeev.crawler.dbs;

import org.andreyfadeev.crawler.interfaces.Index;
import org.andreyfadeev.crawler.dbs.redis.JedisIndex;
import org.andreyfadeev.crawler.dbs.redis.LettuceIndex;
import org.andreyfadeev.crawler.dbs.sql.RatesDatabase;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;

public class DBFactory {
    private static final String JDBC_PREFIX = "jdbc:";
    public static String MYSQL_ADRESS;
    public static String MYSQL_USERNAME;
    public static String MYSQL_PASSWORD;
    public static String REDIS_HOST;
    public static Integer REDIS_PORT;
    public static int REDIS_TIMEOUT = 60000;
    private RatesDatabase ratesDatabase;
    private RedisClient lettuceClient;
    private JedisPool jedisPool;
    private LettuceIndex lettuceIndex;
    private JedisIndex jedisIndex;

    public RatesDatabase getRatesDb() throws SQLException {
            if (ratesDatabase == null) {
                Connection conn = DriverManager.getConnection(
                        JDBC_PREFIX + MYSQL_ADRESS, MYSQL_USERNAME, MYSQL_PASSWORD);
                ratesDatabase = new RatesDatabase(conn);
            }
        return ratesDatabase;
    }

    public Index getJedisIndex() {
        if (jedisIndex == null) {
            jedisPool = new JedisPool(REDIS_HOST, REDIS_PORT);
            jedisIndex = new JedisIndex(jedisPool.getResource());
        }

        return jedisIndex;
    }

    public Index getLettuceIndex() {
        if (lettuceIndex == null) {
            RedisURI redisURI = RedisURI.builder()
                                        .redis(REDIS_HOST, REDIS_PORT)
                                        .withTimeout(Duration.ofMillis(REDIS_TIMEOUT))
                                        .build();
            lettuceClient = RedisClient.create(redisURI);
//            String ltString = "redis://" + REDIS_HOST + ":" + REDIS_PORT + "/0";
//            lettuceClient = RedisClient.create(ltString);
            lettuceIndex = new LettuceIndex(lettuceClient.connect());
        }

        return lettuceIndex;
    }

    public Index getIndex() {
        return getLettuceIndex();
    }

    public void close() {
        if (lettuceClient != null) {
            lettuceClient.shutdown();
        }
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
