import com.allendowney.thinkdast.HtmlCrawler;
import com.allendowney.thinkdast.LinksLoader;
import com.allendowney.thinkdast.PageFetcher;
import com.allendowney.thinkdast.TermCounter;
import com.allendowney.thinkdast.interfaces.Crawler;
import com.allendowney.thinkdast.interfaces.Index;
import com.allendowney.thinkdast.interfaces.TermContainer;
import dbs.DBFactory;
import dbs.redis.JedisIndex;
import dbs.redis.JedisMaker;
import dbs.sql.RatesDatabase;
import dbs.sql.orm.Page;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.*;

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

    private static void parseUnscannedPages(RatesDatabase ratesDb) throws SQLException, IOException {
        Crawler crawler = new HtmlCrawler(new JedisIndex(JedisMaker.make()));
        for (int siteId : ratesDb.getSiteIds()) {
            Set<String> pages = ratesDb.getBunchOfUnscannedPages(siteId, 1000);
            ratesDb.updateLastScanDatesByUrl(pages, new Timestamp(System.currentTimeMillis()));
            try {
                Set<String> unscanned = crawler.crawlPages(pages);
                for (String page : unscanned) {
                    System.out.println("Unscanned! Page url is: " + page);
                }
                ratesDb.updateLastScanDatesByUrl(unscanned, null);
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
                                         RatesDatabase db) throws SQLException {
        Integer siteId = db.getSiteIdByLink(url);
        System.out.println("Adding links to DB from " + url);
        if (siteId != null) {
            db.insertRowsInPagesTable(links, siteId, null);
        }
    }

    private static void parseInput(String[] args) throws SQLException, IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i]);
        }
        String arguments = sb.toString();

        if (arguments.contains("-irl"))
            insertLinksToRobotsPages(DBFactory.getRatesDb());
        if (arguments.contains("-frl"))
            fetchLinksFromRobotsTxt(DBFactory.getRatesDb());
        if (arguments.contains("-fsl"))
            fetchLinksFromSitmaps(DBFactory.getRatesDb());
        if (arguments.contains("-pul"))
            parseUnscannedPages(DBFactory.getRatesDb());

        if (arguments.isEmpty()) {
            System.out.println("Usage: java Crawler -<param>");
            System.out.println("List of available parameters:");
            System.out.println("-irl - insert links to robots.txt in database for found new sites;");
            System.out.println("-frl - fetch links from robots.txt's and save them to the database;");
            System.out.println("-fsl - fetch links from unscanned sitemaps, found in db and save them;");
            System.out.println("-pul - parse unscanned pages, found in database, and save words from them.");
        }
    }

    public static void main(String[] args) {
        try {
            RatesDatabase ratesDb = DBFactory.getRatesDb();
            Map<Integer, Set<String>> keywords = ratesDb.getPersonsWithKeywords();
            for (Integer personId: keywords.keySet()) {
                System.out.println("Person with id " + personId + " keywords:");
                for (String word : keywords.get(personId)) {
                    System.out.println(word);
                }
            }
            Map<Integer, Integer> pageRanks = new HashMap<>();
            pageRanks.put(1,3);
            ratesDb.insertPersonsPageRanks(1,pageRanks);
//            insertLinksToRobotsPages(ratesDb);
//            fetchLinksFromRobotsTxt(ratesDb);
//            fetchLinksFromSitmaps(ratesDb);
//            parseUnscannedPages(ratesDb);
//            JedisIndex jedis = new JedisIndex(JedisMaker.make());
//            jedis.deleteAllKeys();
//            jedis.printIndex();
//            parseInput(args);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
