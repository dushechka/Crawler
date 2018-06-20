package org.andreyfadeev.crawler;

import de.l3s.boilerpipe.extractors.ArticleExtractor;
import org.andreyfadeev.crawler.dbs.redis.LettuceIndex;
import org.andreyfadeev.crawler.interfaces.Crawler;
import org.andreyfadeev.crawler.interfaces.Index;
import org.andreyfadeev.crawler.dbs.DBFactory;
import org.andreyfadeev.crawler.dbs.sql.RatesDatabase;
import org.andreyfadeev.crawler.dbs.sql.orm.Page;
import org.andreyfadeev.crawler.interfaces.TermContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public static final String MYSQL_PROPS_FILENAME = "/mysql_props.txt";
    public static final String REDIS_PROPS_FILENAME = "/redis_props.txt";
    private static int MAX_PAGES_PER_SCAN_CYCLE = 10;
    private final DBFactory dbFactory;

    public WebCrawler(DBFactory dbFactory) {
        this.dbFactory = dbFactory;
    }

    /**
     * Inserts links to the robots.txt file
     * for sites, that have only one link in DB.
     *
     * @param ratesDb   Database to work with
     * @throws SQLException if database can't
     *                      execute some of the queries.
     */
    private void insertLinksToRobotsPages(RatesDatabase ratesDb) throws SQLException {
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

    private Set<Page> selectUnscannedPages(Set<Page> pages) {
        Set<Page> unscanned = new HashSet<>();
        for (Page page : pages) {
            if (page.getLastScanDate() == null) {
                unscanned.add(page);
            }
        }
        return unscanned;
    }

    private void fetchLinksFromRobotsTxt(RatesDatabase ratesDb) throws SQLException {
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

    private void fetchLinksFromSitmaps(RatesDatabase ratesDb) throws SQLException {
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

    private void parseUnscannedPages(RatesDatabase ratesDb, Index index) throws Exception {
        System.out.println("Maximum pages to scan per cycle: " + MAX_PAGES_PER_SCAN_CYCLE);
        System.out.println("Redis timeout: " + DBFactory.REDIS_TIMEOUT);
        Map<Integer, Set<String>> keywords = ratesDb.getPersonsWithKeywords();
        Crawler crawler = new HtmlCrawler();
        int errCounter = 0;
        for (int siteId : ratesDb.getSiteIds()) {
            Set<String> links;
            do {
                System.out.println("\nGetting links for site with id=" + siteId);
                links = ratesDb.getBunchOfUnscannedLinks(siteId, MAX_PAGES_PER_SCAN_CYCLE);
                ratesDb.updateLastScanDatesByUrl(links, new Timestamp(System.currentTimeMillis()));
                System.out.println();
                Set<TermContainer> parsed;
                try {
                    parsed = crawler.crawlPages(links, index);
                    errCounter = 0;
                    updatePersonsPageRanks(keywords, parsed, ratesDb);
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
        index.close();
    }

    private void updatePersonsPageRanks(Map<Integer, Set<String>> keywords,
                                        Set<TermContainer> pageRanks, RatesDatabase rdb) throws SQLException {
        Map<Integer, Map<String, Integer>> personsPageRanks = new HashMap<>();
        for (TermContainer tc : pageRanks) {
            for (Integer personId : keywords.keySet()) {
                Map<String, Integer> ppr = new HashMap<>();
                for (String word : keywords.get(personId)) {
                    Integer count = tc.get(word.toLowerCase());
                    if (count > 0) {
                        System.out.println("Found entries for word " + word + ":");
                        System.out.printf("URL: %S, Count: %s\n", tc.getLabel(), count);
                        ppr.merge(tc.getLabel(), count, (first, second) -> first + second);
                    }
                }
                if (!ppr.isEmpty()) {
                    personsPageRanks.put(personId, ppr);
                }
            }
        }
        if (!personsPageRanks.isEmpty()) {
            System.out.println("Saving counts to DB...");
            for (Integer personId : personsPageRanks.keySet()) {
                rdb.insertPersonPageRanks(personId, personsPageRanks.get(personId));
            }
        }
    }

    private void reindexPageRanks(RatesDatabase ratesDb, Index index) throws SQLException{
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
        index.close();
    }

    /**
     *
     * @param url   Page url in DB, by which we can get site ID
     * @param links
     * @param db
     * @throws SQLException
     */
    private void saveLinksToDb(String url, Set<String> links,
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
    private void saveLinksToDb(String url, Map<String, Timestamp> links,
                                      RatesDatabase db) throws SQLException {
        Integer siteId = db.getSiteIdByLink(url);
        System.out.println("Adding links to DB from " + url);
        if (siteId != null) {
            db.insertRowsInPagesTable(links, siteId, null);
        }
    }

    private void updateAllPersonsPageRanks(RatesDatabase ratesDb, Index index) throws SQLException {
        Map<Integer, Map<String, Integer>> personPageRanks = getPageRanksFromIndex(ratesDb, index);
        for (Integer personId : personPageRanks.keySet()) {
            Map<String, Integer> ranks = personPageRanks.get(personId);
            ratesDb.insertPersonPageRanks(personId, ranks);
        }
    }

    private Map<Integer, Map<String, Integer>> getPageRanksFromIndex(
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

    private void putOrUpdate(Map<String, Integer> source, Map<String, Integer> target) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            target.merge(key, value, (first, second) -> first + second);
        }
    }

    private void parseInput(String[] args) throws Exception {
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
                    reindexPageRanks(dbFactory.getRatesDb(), dbFactory.getIndex());
                if (arg.contains("-irl"))
                    insertLinksToRobotsPages(dbFactory.getRatesDb());
                if (arg.contains("-frl"))
                    fetchLinksFromRobotsTxt(dbFactory.getRatesDb());
                if (arg.contains("-fsl"))
                    fetchLinksFromSitmaps(dbFactory.getRatesDb());
                if (arg.contains("-pul"))
                    parseUnscannedPages(dbFactory.getRatesDb(), dbFactory.getIndex());
                if (arg.contains("-all")) {
                    runWholeProgramCycle();
                }
            }
        }
    }

    private void runWholeProgramCycle() throws Exception {
        RatesDatabase rdb = dbFactory.getRatesDb();
        reindexPageRanks(rdb, dbFactory.getIndex());
        insertLinksToRobotsPages(rdb);
        fetchLinksFromRobotsTxt(rdb);
        fetchLinksFromSitmaps(rdb);
        parseUnscannedPages(rdb, dbFactory.getIndex());
    }

    private void setProperties() throws IOException{
        String filename = MYSQL_PROPS_FILENAME;
        InputStream in = getClass().getResourceAsStream(MYSQL_PROPS_FILENAME);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        try {
            DBFactory.MYSQL_ADRESS = br.readLine();
            DBFactory.MYSQL_USERNAME = br.readLine();
            DBFactory.MYSQL_PASSWORD = br.readLine();
            br.close();

            filename = REDIS_PROPS_FILENAME;
            in = getClass().getResourceAsStream(filename);
            br = new BufferedReader(new InputStreamReader(in));
            DBFactory.REDIS_HOST = br.readLine();
            DBFactory.REDIS_PORT = Integer.parseInt(br.readLine());
            br.close();
        } catch (IOException exc) {
            System.out.println("Can't read file! Filename: " + filename);
            throw exc;
        }
    }

    public static void main(String[] args) {
        try {
            DBFactory dbFactory = new DBFactory();
            WebCrawler wc = new WebCrawler(dbFactory);
            wc.setProperties();
            wc.parseInput(args);
//            URL url = new URL("https://www.pravda.ru/news/society/20-06-2018/1386876-reforma-0/");
//            System.out.println(ArticleExtractor.INSTANCE.getText(url));
            dbFactory.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
