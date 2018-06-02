import dbs.DBFactory;
import dbs.sql.RatesDatabase;

import java.sql.*;
import java.util.Set;

public class WebCrawler {
    private static final String ROBOTS_TXT_APPENDIX = "/robots.txt";

    public static void main(String[] args) throws Exception {
        try {
            RatesDatabase ratesDb = DBFactory.getRatesDb();
            Set<Integer> sites = ratesDb.getSitesWithSinglePages();
            for (Integer siteId : sites) {
                String address = ratesDb.getSiteAddress(siteId);
                if (address != null) {
                    System.out.println(address);
                    String robotsAddress = address + ROBOTS_TXT_APPENDIX;
                    ratesDb.insertRowInPages(robotsAddress, siteId,
                                                new Timestamp(System.currentTimeMillis()));
                }
            }
        } catch (SQLException exc) {
            exc.printStackTrace();
        }
    }
}
