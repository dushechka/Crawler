package org.andreyfadeev.crawler.dbs;

import org.andreyfadeev.crawler.interfaces.Index;
import org.andreyfadeev.crawler.dbs.redis.JedisIndex;
import org.andreyfadeev.crawler.dbs.redis.LettuceIndex;
import org.andreyfadeev.crawler.dbs.sql.RatesDatabase;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import redis.clients.jedis.Jedis;

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
    private Index jedisIndex;

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
            jedisIndex = new JedisIndex(new Jedis(REDIS_HOST, REDIS_PORT));
        }
        return jedisIndex;
    }

    public Index getLettuceIndex() {
        if (lettuceClient == null) {
            RedisURI redisURI = RedisURI.builder()
                                        .redis(REDIS_HOST, REDIS_PORT)
                                        .withTimeout(Duration.ofMillis(REDIS_TIMEOUT))
                                        .build();
            lettuceClient = RedisClient.create(redisURI);
        }
        return new LettuceIndex(lettuceClient);
    }
}
