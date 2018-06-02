import dbs.DBFactory;
import dbs.sql.RatesDatabase;

import java.sql.*;
import java.util.Set;

public class WebCrawler {

    private static final String DB_ADRESS = "jdbc:mysql://localhost:3306/searchandratewords";
    private static final String USER_NAME = "crawler";
    private static final String PASSWORD = "123";

    public static void main(String[] args) throws Exception {
        try {
            RatesDatabase ratesDb = DBFactory.getRatesDb();
            Set<Integer> sites = ratesDb.getSitesWithSinglePages();
            for (Integer siteId : sites) {
                if (siteId != null) {
                    System.out.println(ratesDb.getSiteAddress(siteId));
                    ratesDb.insertRowInPages(ratesDb.getSiteAddress(siteId), siteId, new Date(0));
                }
            }
        } catch (SQLException exc) {
            exc.printStackTrace();
        }
    }
}
