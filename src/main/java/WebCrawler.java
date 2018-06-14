import com.allendowney.thinkdast.HtmlCrawler;
import com.allendowney.thinkdast.LinksLoader;
import com.allendowney.thinkdast.interfaces.Crawler;
import com.allendowney.thinkdast.interfaces.Index;
import dbs.DBFactory;
import dbs.sql.RatesDatabase;
import dbs.sql.orm.Page;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.allendowney.thinkdast.LinksLoader.ROBOTS_TXT_APPENDIX;

public class WebCrawler {

    private static int MAX_PAGES_PER_SCAN_CYCLE = 10;

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
        Index index = null;
        System.out.println("Maximum pages to scan per cycle: " + MAX_PAGES_PER_SCAN_CYCLE);
        System.out.println("Redis timeout: " + DBFactory.REDIS_TIMEOUT);
        Crawler crawler = new HtmlCrawler();
        int errCounter = 0;
        for (int siteId : ratesDb.getSiteIds()) {
            Set<String> links;
            do {
                links = ratesDb.getBunchOfUnscannedLinks(siteId, MAX_PAGES_PER_SCAN_CYCLE);
                ratesDb.updateLastScanDatesByUrl(links, new Timestamp(System.currentTimeMillis()));
                System.out.println("Preparing to scan pages");
                try {
                    index = DBFactory.getIndex();
                    Set<String> unscanned = crawler.crawlPages(links, DBFactory.getIndex());
                    index.close();
                    for (String lnk : unscanned) {
                        System.out.println("Unscanned! Page url is: " + lnk);
                    }
                    links.removeAll(unscanned);
                    ratesDb.updateLastScanDatesByUrl(unscanned, null);
                    errCounter = 0;
                } catch (IOException exc) {
                    errCounter++;
                    ratesDb.updateLastScanDatesByUrl(links, null);
                    exc.printStackTrace();
                    if (errCounter > 7 ) {
                        throw new IOException(exc);
                    }
                }
                index = DBFactory.getIndex();
                updatePersonsPageRanks(links, ratesDb, DBFactory.getIndex());
                index.close();
            } while (!links.isEmpty());
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

    private static void updateAllPersonsPageRanks(RatesDatabase ratesDb, Index index) throws SQLException {
        Map<Integer, Map<String, Integer>> personPageRanks = getPageRanksFromIndex(ratesDb, index);
        for (Integer personId : personPageRanks.keySet()) {
            Map<String, Integer> ranks = personPageRanks.get(personId);
            ratesDb.insertPersonsPageRanks(personId, ranks);
        }
    }

    private static void updatePersonsPageRanks(Set<String> links,
                                               RatesDatabase ratesDb, Index index) throws SQLException {
            Map<Integer, Map<String, Integer>> personPageRanks = getPageRanksFromIndex(ratesDb, index);
            Set<String> absentLinks = new HashSet<>();
            for (Integer personId : personPageRanks.keySet()) {
                Map<String, Integer> ranks = personPageRanks.get(personId);
                for (Map.Entry<String, Integer> rank : ranks.entrySet()) {
                    String link = rank.getKey();
                    if (!links.contains(link)) {
                        absentLinks.add(link);
                    }
                }
                for (String absLnk : absentLinks) {
                    ranks.remove(absLnk);
                }
                ratesDb.insertPersonsPageRanks(personId, ranks);
            }
    }

    private static Map<Integer, Map<String, Integer>> getPageRanksFromIndex(
                RatesDatabase ratesDb, Index index) throws SQLException {
        Map<Integer, Set<String>> keywords = ratesDb.getPersonsWithKeywords();
        Map<Integer, Map<String, Integer>> pageRanks = new HashMap<>();
        for (Integer personId: keywords.keySet()) {
            Map<String, Integer> personPageRanks = new HashMap<>();
            for (String word : keywords.get(personId)) {
                System.out.println("Getting counts for word: " + word);
                Map<String, Integer> counts = index.getCounts(word.toLowerCase());
                System.out.println("Counts: " + counts);
                putOrUpdate(index.getCounts(word.toLowerCase()), personPageRanks);
            }
            for (String url : personPageRanks.keySet()) {
                System.out.println(url + " : " + personPageRanks.get(url));
            }
            pageRanks.put(personId, personPageRanks);
        }
        return pageRanks;
    }

    private static void putOrUpdate(Map<String, Integer> source, Map<String, Integer> target) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            target.merge(key, value, (first, second) -> first + second);
        }
    }

    private static void parseInput(String[] args) throws SQLException, IOException {
        if (args.length == 0) {
            System.out.println("Usage: java Crawler -<param>");
            System.out.println("List of available parameters:");
            System.out.println("-irl - insert links to robots.txt in database for found new sites;");
            System.out.println("-frl - fetch links from robots.txt's and save them to the database;");
            System.out.println("-fsl - fetch links from unscanned sitemaps, found in db and save them;");
            System.out.println("-pul - parse unscanned pages, found in database, and save words from them;");
            System.out.println("-all - run whole cycle of crawling;");
            System.out.println("-rtm <number> - set redis time-out;");
            System.out.println("-lpc <number> - set number of links to process at a time.");
        } else {

            for (int i = 0; i < args.length; i++) {
                if (args[i].contains("-rtm")) {
                    try {
                        DBFactory.REDIS_TIMEOUT = Integer.parseInt(args[i + 1]);
                    } catch (NumberFormatException exc) {
                        System.out.println("Wrong number format for -rtm parameter!");
                    }
                }
                if (args[i].contains("-lpc")) {
                    try {
                        MAX_PAGES_PER_SCAN_CYCLE = Integer.parseInt(args[i + 1]);
                    } catch (NumberFormatException exc) {
                        System.out.println("Wrong number format for -lpc parameter!");
                    }
                }
            }

            for (String arg : args) {
                if (arg.contains("-irl"))
                    insertLinksToRobotsPages(DBFactory.getRatesDb());
                if (arg.contains("-frl"))
                    fetchLinksFromRobotsTxt(DBFactory.getRatesDb());
                if (arg.contains("-fsl"))
                    fetchLinksFromSitmaps(DBFactory.getRatesDb());
                if (arg.contains("-pul"))
                    parseUnscannedPages(DBFactory.getRatesDb());
                if (arg.contains("-all")) {
                    runWholeProgramCycle();
                }
            }
        }
    }

    private static void runWholeProgramCycle() throws SQLException, IOException {
        RatesDatabase rdb = DBFactory.getRatesDb();
        insertLinksToRobotsPages(rdb);
        fetchLinksFromRobotsTxt(rdb);
        fetchLinksFromSitmaps(rdb);
        parseUnscannedPages(rdb);
    }

    public static void main(String[] args) {
        try {
            RatesDatabase ratesDb = DBFactory.getRatesDb();
//            insertLinksToRobotsPages(ratesDb);
//            fetchLinksFromRobotsTxt(ratesDb);
//            fetchLinksFromSitmaps(ratesDb);
//            parseUnscannedPages(ratesDb);
//            JedisIndex index = new JedisIndex(JedisMaker.make());
//            index.deleteAllKeys();
//            index.printIndex();
            parseInput(args);
//            updateAllPersonsPageRanks(ratesDb, index);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
