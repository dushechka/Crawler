package dbs;

import dbs.sql.RatesDatabase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBFactory {
    private static final String DB_ADRESS = "jdbc:mysql://localhost:3306/searchandratewords";
    private static final String USER_NAME = "crawler";
    private static final String PASSWORD = "123";
    private static RatesDatabase ratesDatabase = null;

    public static RatesDatabase getRatesDb() throws SQLException {
            if (ratesDatabase == null) {
                Connection conn = DriverManager.getConnection(DB_ADRESS, USER_NAME, PASSWORD);
                ratesDatabase = new RatesDatabase(conn);
            }
        return ratesDatabase;
    }
}
