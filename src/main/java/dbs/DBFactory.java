package dbs;

import com.allendowney.thinkdast.interfaces.Index;
import dbs.redis.JedisIndex;
import dbs.sql.RatesDatabase;
import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBFactory {
    public static final String DB_ADRESS = "jdbc:mysql://localhost:3306/searchandratewords";
    public static final String USER_NAME = "crawler";
    public static final String PASSWORD = "123";
    public static final String REDIS_HOST = "192.168.56.101";
    public static final int REDIS_PORT = 6379;
    public static int REDIS_TIMEOUT = 10000;
    public static RatesDatabase ratesDatabase = null;

    public static RatesDatabase getRatesDb() throws SQLException {
            if (ratesDatabase == null) {
                Connection conn = DriverManager.getConnection(DB_ADRESS, USER_NAME, PASSWORD);
                ratesDatabase = new RatesDatabase(conn);
            }
        return ratesDatabase;
    }

    public static Index getIndex() {
        return new JedisIndex(new Jedis(REDIS_HOST, REDIS_PORT));
    }
}
