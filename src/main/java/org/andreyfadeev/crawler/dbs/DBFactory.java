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
    public static final String DB_ADRESS = "jdbc:mysql://localhost:3306/searchandratewords";
    public static final String USER_NAME = "crawler";
    public static final String PASSWORD = "123";
    public static final String REDIS_HOST = "192.168.56.101";
    public static final int REDIS_PORT = 6379;
    private static final String COLON = ":";
    public static int REDIS_TIMEOUT = 60000;
    private RatesDatabase ratesDatabase = null;
    private RedisClient lettuceClient = null;
    private Index jedisIndex = null;

    public RatesDatabase getRatesDb() throws SQLException {
            if (ratesDatabase == null) {
                Connection conn = DriverManager.getConnection(DB_ADRESS, USER_NAME, PASSWORD);
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
