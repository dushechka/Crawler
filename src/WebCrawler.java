import com.allendowney.thinkdast.LinksLoader;
import dbs.DBFactory;
import dbs.sql.RatesDatabase;
import dbs.sql.orm.Page;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import static com.allendowney.thinkdast.LinksLoader.ROBOTS_TXT_APPENDIX;

public class WebCrawler {

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
                String address = LinksLoader.getSiteAddress(page.getUrl());
                if (LinksLoader.isSiteAvailable(address)) {
                    System.out.println("Adding robots.txt link for " + address);
                    String robotsAddress = address + ROBOTS_TXT_APPENDIX;
                    ratesDb.insertRowInPagesTable(new URL(robotsAddress), page.getSiteId(), null);
                }
            } catch (MalformedURLException | UnknownHostException e) {
                e.printStackTrace();
            } catch (SQLException | IOException exc) {
                exc.printStackTrace();
                ratesDb.updateLastScanDate(page.getiD(), null);
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

    public static void main(String[] args) {
        try {
            RatesDatabase ratesDb = DBFactory.getRatesDb();
//            insertLinksToRobotsPages(ratesDb);
            Set<String> robotsTxtLinks = ratesDb.getRobotsTxtPages();
            LinksLoader ln = new LinksLoader();
            for (String url : robotsTxtLinks) {
                ln.getPagesFromRobotsTxt(url);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

}
