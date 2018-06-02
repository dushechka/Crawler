import dbs.DBFactory;
import dbs.sql.RatesDatabase;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.Set;

public class WebCrawler {
    private static final String ROBOTS_TXT_APPENDIX = "/robots.txt";

    /**
     * Inserts links to the robots.txt file
     * for sites, that have only one link in DB.
     *
     * @param ratesDb   Database to work with
     * @throws SQLException if database can't
     *                      execute some of the queries.
     */
    private static void insertLinksToRobotsPages(RatesDatabase ratesDb) throws SQLException {
        Set<Integer> sites = ratesDb.getSitesWithSinglePages();
        for (Integer siteId : sites) {
            String address = ratesDb.getSiteAddress(siteId);
            if (address != null) {
                System.out.println(address);
                String robotsAddress = address + ROBOTS_TXT_APPENDIX;
                try {
                    ratesDb.insertRowInPages(new URL(robotsAddress), siteId,
                            new Timestamp(System.currentTimeMillis()));
                } catch (MalformedURLException exc) {
                    System.out.println("Found malformed URL for a site! SiteID =" + siteId);
                    System.out.println("Can't insert robots.txt link.");
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            RatesDatabase ratesDb = DBFactory.getRatesDb();
            insertLinksToRobotsPages(ratesDb);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

}
