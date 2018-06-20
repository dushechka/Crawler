package org.andreyfadeev.crawler;

import de.l3s.boilerpipe.extractors.ArticleExtractor;
import org.andreyfadeev.crawler.HtmlCrawler;
import org.andreyfadeev.crawler.LinksLoader;
import org.andreyfadeev.crawler.dbs.redis.JedisIndex;
import org.andreyfadeev.crawler.interfaces.Crawler;
import org.andreyfadeev.crawler.interfaces.Index;
import org.andreyfadeev.crawler.dbs.DBFactory;
import org.andreyfadeev.crawler.dbs.sql.RatesDatabase;
import org.andreyfadeev.crawler.dbs.sql.orm.Page;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.andreyfadeev.crawler.LinksLoader.ROBOTS_TXT_APPENDIX;

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
                        ratesDb.updateLastScanDate(page.getiD(), null);
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

    private static void parseUnscannedPages(RatesDatabase ratesDb, Index index) throws Exception {
        System.out.println("Maximum pages to scan per cycle: " + MAX_PAGES_PER_SCAN_CYCLE);
        System.out.println("Redis timeout: " + DBFactory.REDIS_TIMEOUT);
        Crawler crawler = new HtmlCrawler();
        int errCounter = 0;
        for (int siteId : ratesDb.getSiteIds()) {
            Set<String> links;
            do {
                System.out.println("Getting links for site with id=" + siteId);
                links = ratesDb.getBunchOfUnscannedLinks(siteId, MAX_PAGES_PER_SCAN_CYCLE);
                ratesDb.updateLastScanDatesByUrl(links, new Timestamp(System.currentTimeMillis()));
                System.out.println("Preparing to scan pages");
                Set<String> unscanned;
                try {
                    unscanned = crawler.crawlPages(links, index);
                    errCounter = 0;
                    links.removeAll(unscanned);
                    updatePersonsPageRanks(links, ratesDb, index);
                } catch (Exception exc) {
                    errCounter++;
                    ratesDb.updateLastScanDatesByUrl(links, null);
                    exc.printStackTrace();
                    if (errCounter > 7 ) {
                        throw new Exception(exc);
                    }
                }
            } while (!links.isEmpty());
        }
    }

    private static void reindexPageRanks(RatesDatabase ratesDb, Index index) throws SQLException{
        Map<Integer, Map<String, Integer>> personsPageRanks = ratesDb.getPersonsPageRanks();
        Map<Integer, Map<String, Integer>> indexPpr = getPageRanksFromIndex(ratesDb, index);
        for (Integer personId : personsPageRanks.keySet()) {
            Map<String, Integer> dbPageRandks = personsPageRanks.get(personId);
            Map<String, Integer> indexPageRanks = indexPpr.get(personId);
            for (String url : dbPageRandks.keySet()) {
                indexPageRanks.remove(url);
            }
            ratesDb.insertPersonPageRanks(personId, indexPageRanks);
        }
    }

    /**
     *
     * @param url   Page url in DB, by which we can get site ID
     * @param links
     * @param db
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

    /**
     *
     * @param url   page url in DB, by which we can get site ID
     * @param links page urls with foundDateTime field timestamps
     * @param db
     * @throws SQLException
     */
    private static void saveLinksToDb(String url, Map<String, Timestamp> links,
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
            ratesDb.insertPersonPageRanks(personId, ranks);
        }
    }

    private static void updatePersonsPageRanks(Set<String> links,
                                               RatesDatabase ratesDb, Index index) throws SQLException {
        Map<Integer, Map<String, Integer>> personPageRanks = getPageRanksFromIndex(links, ratesDb, index);
        for (Integer personId : personPageRanks.keySet()) {
            Map<String, Integer> ranks = personPageRanks.get(personId);
            ratesDb.insertPersonPageRanks(personId, ranks);
        }
    }

    private static Map<Integer, Map<String, Integer>> getPageRanksFromIndex(
            Set<String> links, RatesDatabase ratesDb, Index index) throws SQLException {
        Map<Integer, Set<String>> keywords = ratesDb.getPersonsWithKeywords();
        Map<Integer, Map<String, Integer>> pageRanks = new HashMap<>();
        for (Integer personId: keywords.keySet()) {
            Map<String, Integer> personPageRanks = new HashMap<>();
            pageRanks.put(personId, personPageRanks);
            for (String keyword : keywords.get(personId)) {
                System.out.println("Getting counts for keyword: " + keyword);
                Set<String> kwl = index.getURLs(keyword.toLowerCase());
                kwl.retainAll(links);
                for (String lnk : kwl) {
                    Integer count = index.getCount(lnk, keyword.toLowerCase());
                    if (count != null) {
                        System.out.println(lnk + ": " + count);
                        personPageRanks.merge(lnk, count, (first, second) -> first + second);
                    }
                }
            }
        }
        return pageRanks;
    }

    private static Map<Integer, Map<String, Integer>> getPageRanksFromIndex(
                        RatesDatabase ratesDb, Index index) throws SQLException {
        Map<Integer, Set<String>> keywords = ratesDb.getPersonsWithKeywords();
        Map<Integer, Map<String, Integer>> pageRanks = new HashMap<>();
        for (Integer personId: keywords.keySet()) {
            Map<String, Integer> personPageRanks = new HashMap<>();
            System.out.println();
            for (String word : keywords.get(personId)) {
                System.out.println("Getting counts for word: " + word);
                Map<String, Integer> counts = index.getCounts(word.toLowerCase());
                putOrUpdate(counts, personPageRanks);
            }
            System.out.println("\nAll page ranks for person with ID = " + personId);
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

    private static void parseInput(String[] args, DBFactory dbFactory) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java Crawler -<param>\n");
            System.out.println("List of available parameters:");
            System.out.println("-rdx - reindex persons page ranks from previously saved vocabularies;");
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
                if (arg.contains("-rdx"))
                    reindexPageRanks(dbFactory.getRatesDb(), dbFactory.getJedisIndex());
                if (arg.contains("-irl"))
                    insertLinksToRobotsPages(dbFactory.getRatesDb());
                if (arg.contains("-frl"))
                    fetchLinksFromRobotsTxt(dbFactory.getRatesDb());
                if (arg.contains("-fsl"))
                    fetchLinksFromSitmaps(dbFactory.getRatesDb());
                if (arg.contains("-pul"))
                    parseUnscannedPages(dbFactory.getRatesDb(), dbFactory.getJedisIndex());
                if (arg.contains("-all")) {
                    runWholeProgramCycle(dbFactory);
                }
            }
        }
    }

    private static void runWholeProgramCycle(DBFactory dbFactory) throws Exception {
        RatesDatabase rdb = dbFactory.getRatesDb();
        reindexPageRanks(rdb, dbFactory.getJedisIndex());
        insertLinksToRobotsPages(rdb);
        fetchLinksFromRobotsTxt(rdb);
        fetchLinksFromSitmaps(rdb);
        parseUnscannedPages(rdb, dbFactory.getJedisIndex());
    }

    public static void main(String[] args) {
        try {
            DBFactory dbFactory = new DBFactory();
            parseInput(args, dbFactory);
//            URL url = new URL("https://lenta.ru/news/2018/05/24/putin_vs_trump/");
//            System.out.println(ArticleExtractor.INSTANCE.getText(url));
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
