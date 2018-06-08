import com.allendowney.thinkdast.HtmlCrawler;
import com.allendowney.thinkdast.LinksLoader;
import com.allendowney.thinkdast.PageFetcher;
import com.allendowney.thinkdast.TermCounter;
import com.allendowney.thinkdast.interfaces.Crawler;
import com.allendowney.thinkdast.interfaces.Index;
import com.allendowney.thinkdast.interfaces.TermContainer;
import dbs.DBFactory;
import dbs.sql.RatesDatabase;
import dbs.sql.orm.Page;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        for (Page page : pages) {
            if (ratesDb.getLastScanDate(page.getUrl()) == null) {
                ratesDb.updateLastScanDate(page.getiD(), new Timestamp(System.currentTimeMillis()));
                try {
                    String address = LinksLoader.getSiteAddress(page.getUrl());
                    if (LinksLoader.isSiteAvailable(address)) {
                        System.out.println("Adding robots.txt link for " + address);
                        String robotsAddress = address + ROBOTS_TXT_APPENDIX;
                        ratesDb.insertRowInPagesTable(robotsAddress, page.getSiteId(), null);
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (SQLException | IOException exc) {
                    exc.printStackTrace();
                    ratesDb.updateLastScanDate(page.getiD(), null);
                }
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

    private static void fetchLinksFromRobotsTxt(RatesDatabase ratesDb) throws SQLException {
        Set<String> robotsTxtLinks = ratesDb.getUnscannedRobotsTxtLinks();
        LinksLoader ln = new LinksLoader();
        for (String url : robotsTxtLinks) {
            if (ratesDb.getLastScanDate(url) == null) {
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
    }

    private static void fetchLinksFromSitmaps(RatesDatabase ratesDb) throws SQLException {
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

    private static void parseUnscannedPages(RatesDatabase ratesDb) throws SQLException {
        Crawler crawler = new HtmlCrawler(new Index() {
            @Override
            public boolean isIndexed(String url) {
                return false;
            }

            @Override
            public void add(String term, String url) {

            }

            @Override
            public Set<String> getURLs(String term) {
                return null;
            }

            @Override
            public Map<String, Integer> getCounts(String term) {
                return null;
            }

            @Override
            public Integer getCount(String url, String term) {
                return null;
            }

            @Override
            public List<String> putTerms(TermContainer tc) {
                System.out.println(tc);
                return null;
            }

            @Override
            public Set<String> termSet() {
                return null;
            }
        });
        for (int siteId : ratesDb.getSiteIds()) {
            Set<String> pages = ratesDb.getBunchOfUnscannedPages(siteId, 1000);
            ratesDb.updateLastScanDatesByUrl(pages, new Timestamp(System.currentTimeMillis()));
            try {
                for (String page : crawler.crawlPages(pages)) {
                    System.out.println(page);
                }
            } catch (NullPointerException exc) {
                ratesDb.updateLastScanDatesByUrl(pages, null);
                exc.printStackTrace();
            }
        }
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
//            insertLinksToRobotsPages(ratesDb);
//            fetchLinksFromRobotsTxt(ratesDb);
//            fetchLinksFromSitmaps(ratesDb);
            parseUnscannedPages(ratesDb);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
