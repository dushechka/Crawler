import dbs.DBFactory;
import dbs.sql.RatesDatabase;
import dbs.sql.orm.Page;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class WebCrawler {
    private static final String PROTOCOL_DELIMITER = "://";
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
        Set<Page> pages = selectUnscannedPages(ratesDb.getSinglePages());
        Set<Integer> pageIds = new HashSet<>();
        for (Page page : pages) {
            pageIds.add(page.getiD());
        }
        ratesDb.updateLastScanDates(pageIds, new Timestamp(System.currentTimeMillis()));
        for (Page page : pages) {
            try {
                String address = getSiteAddress(page.getUrl());
                System.out.println(address);
                String robotsAddress = address + ROBOTS_TXT_APPENDIX;
                ratesDb.insertRowInPagesTable(new URL(robotsAddress), page.getSiteId(), null);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }

    private static Set<Page> selectUnscannedPages(Set<Page> pages) {
        Set<Page> unscanned = new HashSet<>();
        for (Page page : pages) {
            if (page.getLastScanDate() == null) {
                unscanned.add(page);
            }
        }
        return unscanned;
    }

    /**
     * Takes arbitrary link to the site and returns it's address
     * if form, like http://example.com, with given links protocol.
     *
     * @param link  Arbitrary link to the site
     * @return      Site's common address
     * @throws MalformedURLException if site link is malformed.
     */
    private static String getSiteAddress(String link) throws MalformedURLException {
        URL url = new URL(link);
        return url.getProtocol() + PROTOCOL_DELIMITER + url.getHost();
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
