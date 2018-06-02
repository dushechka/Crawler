import dbs.DBFactory;
import dbs.sql.RatesDatabase;

import java.sql.*;
import java.util.Set;

public class WebCrawler {
    public static void main(String[] args) throws Exception {
        try {
            RatesDatabase ratesDb = DBFactory.getRatesDb();
            Set<Integer> sites = ratesDb.getSitesWithSinglePages();
            for (Integer siteId : sites) {
                String address = ratesDb.getSiteAddress(siteId);
                if (address != null) {
                    System.out.println(address);
                    String robotsAddress = address + "/robots.txt";
                    ratesDb.insertRowInPages(robotsAddress, siteId, null);
                }
            }
        } catch (SQLException exc) {
            exc.printStackTrace();
        }
    }
}
