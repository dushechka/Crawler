import com.allendowney.thinkdast.LinksLoader;
import dbs.DBFactory;
import dbs.sql.RatesDatabase;
import dbs.sql.orm.Page;

import java.io.IOException;
import java.net.MalformedURLException;
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
                    ratesDb.insertRowInPagesTable(robotsAddress, page.getSiteId(), null);
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

    private static void fetchLinksFromRobotsTxt(RatesDatabase ratesDb) throws SQLException, IOException {
        Set<String> robotsTxtLinks = ratesDb.getUnscannedRobotsTxtLinks();
        LinksLoader ln = new LinksLoader();
        for (String url : robotsTxtLinks) {
            try {
                ratesDb.updateLastScanDate(url, new Timestamp(System.currentTimeMillis()));
                Set<String> links = ln.getLinksFromRobotsTxt(url);
                saveLinksToDb(url, links, ratesDb);
            } catch (Exception exc) {
                exc.printStackTrace();
                ratesDb.updateLastScanDate(url, null);
            }
        }
    }

    private static void fetchLinksFromSitmaps(RatesDatabase ratesDb) throws SQLException, IOException {
        LinksLoader ln = new LinksLoader();
        Set<String> links;
        do {
            links = ratesDb.getUnscannedSitemapLinks();
            for (String link : links) {
                System.out.println("Start working with sitemap: " + link);
                if (ratesDb.getLastScanDate(link) == null) {
                    try {
                        ratesDb.updateLastScanDate(link, new Timestamp(System.currentTimeMillis()));
                        saveLinksToDb(link, ln.getLinksFromSitemap(link), ratesDb);
                    } catch (IOException | SQLException exc) {
                        ratesDb.updateLastScanDate(link, null);
                        exc.printStackTrace();
                        return;
                    }
                }
            }
        } while (!links.isEmpty());
    }

    /**
     *
     * @param url   String in DB, by which we can get site ID
     * @param links
     * @param db
     * @throws MalformedURLException
     * @throws SQLException
     */
    private static void saveLinksToDb(String url, Set<String> links,
                                         RatesDatabase db) throws MalformedURLException, SQLException {
        Integer siteId = db.getSiteIdByLink(url);
        System.out.println("Adding links to DB from " + url);
        if (siteId != null) {
            db.insertRowsInPagesTable(links, siteId, null);
        }
    }

    public static void main(String[] args) {
        try {
            RatesDatabase ratesDb = DBFactory.getRatesDb();
            insertLinksToRobotsPages(ratesDb);
            fetchLinksFromRobotsTxt(ratesDb);
            fetchLinksFromSitmaps(ratesDb);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
