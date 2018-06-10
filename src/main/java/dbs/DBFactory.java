package dbs;

import com.allendowney.thinkdast.interfaces.Index;
import dbs.redis.JedisIndex;
import dbs.sql.RatesDatabase;
import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBFactory {
    private static final String DB_ADRESS = "jdbc:mysql://localhost:3306/searchandratewords";
    private static final String USER_NAME = "crawler";
    private static final String PASSWORD = "123";
    private static RatesDatabase ratesDatabase = null;
    private static Index index = null;

    public static RatesDatabase getRatesDb() throws SQLException {
            if (ratesDatabase == null) {
                Connection conn = DriverManager.getConnection(DB_ADRESS, USER_NAME, PASSWORD);
                ratesDatabase = new RatesDatabase(conn);
            }
        return ratesDatabase;
    }

    public static Index getIndex() {
        if (index == null) {
            index = new JedisIndex(new Jedis("192.168.56.101", 6379));
        }
        return index;
    }
}
