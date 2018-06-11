package dbs.sql;

import dbs.sql.orm.ModifiablePage;
import dbs.sql.orm.Page;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RatesDatabase {

    private static final String ID_COLUMN = "ID";
    private static final String PAGES_URL_COLUMN = "URL";
    private static final String PAGES_SITE_ID_COLUMN = "siteID";
    private static final String PAGES_FOUND_DATE_TIME_COLUMN = "foundDateTime";
    private static final String PAGES_LAST_SCAN_DATE_COLUMN = "lastScanDate";
    private static final String COUNT_COLUMN = "COUNT(*)";
    private static final String ROBOTS_TXT_APPENDIX = "robots.txt";
    private static final String SITEMAP = "sitemap";
    private static final String XML = "xml";
    private static final String NAME_COLUMN = "name";
    private final Connection conn;

    public RatesDatabase(Connection conn) {
        this.conn = conn;
    }

    private ResultSet getPersons() throws SQLException {
        Statement stmt = conn.createStatement();
        return stmt.executeQuery("SELECT * FROM persons");
    }

    public Map<Integer, Set<String>> getPersonsWithKeywords() throws SQLException {
        Map<Integer, Set<String>> persons = new HashMap<>();
        PreparedStatement pst = conn.prepareStatement("SELECT name FROM keywords WHERE personID = ?");
        ResultSet rs = getPersons();
        while (rs.next()) {
            int personId = rs.getInt(ID_COLUMN);
            pst.setInt(1, personId);
            ResultSet rst = pst.executeQuery();
            Set<String> keywords = new HashSet<>();
            keywords.add(rs.getString(NAME_COLUMN));
            while (rst.next()) {
                keywords.add(rst.getString(NAME_COLUMN));
            }
            persons.put(personId, keywords);
        }
        rs.close();
        pst.close();
        return persons;
    }

    /**
     * Get rows from "Pages" table grouped by siteID,
     * containing <code>COUNT(*)</code> field.
     */
    private ResultSet getPagesWithCounts() throws SQLException {
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(
                "SELECT *, COUNT(*) FROM pages GROUP BY siteID");
    }


    /**
     * Get rows from "Pages" table where lastScanDate is <code>NULL</code>,
     * grouped by siteID, containing <code>COUNT(*)</code> field.
     */
    private ResultSet getUnscannedPagesWithCounts() throws SQLException {
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(
                "SELECT *, COUNT(*) FROM pages WHERE lastScanDate IS NULL GROUP BY siteID");
    }

    /**
     * Returns amount of site's pages with <code>null</code>
     * in lastScanDate.
     *
     * @param siteId id of the site for which to get pages
     * @param limit  how much pages to extract
     * @return unscanned leaf pages
     * @throws SQLException
     */
    public Set<String> getBunchOfUnscannedLinks(int siteId, int limit) throws SQLException {
        Set<String> links = new HashSet<>();
        PreparedStatement pst = conn.prepareStatement(
                "SELECT URL FROM pages WHERE siteID = ? AND lastScanDate IS NULL LIMIT ?");
        pst.setInt(1, siteId);
        pst.setInt(2, limit);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            String link = rs.getString(PAGES_URL_COLUMN);
            if (!(link.contains(SITEMAP) && link.contains(XML))
                    && !(link.contains(ROBOTS_TXT_APPENDIX))) {
                links.add(link);
            }
        }
        pst.close();
        return links;
    }

    public Set<Page> getBunchOfUnscannedPages(int siteId, int limit) throws SQLException {
        Set<Page> links = new HashSet<>();
        PreparedStatement pst = conn.prepareStatement(
                "SELECT * FROM pages WHERE siteID = ? AND lastScanDate IS NULL LIMIT ?");
        pst.setInt(1, siteId);
        pst.setInt(2, limit);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            String url = rs.getString(PAGES_URL_COLUMN);
            if (!(url.contains(SITEMAP) && url.contains(XML))
                    && !(url.contains(ROBOTS_TXT_APPENDIX))) {
                Page page = new ModifiablePage(
                        rs.getInt(ID_COLUMN),
                        url,
                        rs.getInt(PAGES_SITE_ID_COLUMN),
                        rs.getTimestamp(PAGES_FOUND_DATE_TIME_COLUMN),
                        rs.getTimestamp(PAGES_LAST_SCAN_DATE_COLUMN));
                links.add(page);
            }
        }

        pst.close();
        return links;
    }

    /**
     * Gets unscanned links to robots.txt from database.
     *
     * @return Unscanned links to robots.txt files.
     * @throws SQLException
     */
    public @Nullable
    Set<String> getUnscannedRobotsTxtLinks() throws SQLException {
        Set<String> links = new HashSet<>();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "SELECT URL FROM pages WHERE URL LIKE '%robots.txt' AND lastScanDate IS NULL");
        while (rs.next()) {
            links.add(rs.getString("URL"));
        }
        return links;
    }

    /**
     * Gets unscanned links to sitmep.xml from database.
     *
     * @return Unscanned links to sitmep.xml files.
     * @throws SQLException
     */
    public @Nullable Set<String> getUnscannedSitemapLinks() throws SQLException {
        Set<String> links = new HashSet<>();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "SELECT URL FROM pages WHERE URL LIKE '%sitemap%xml' AND lastScanDate IS NULL");
        while (rs.next()) {
            links.add(rs.getString("URL"));
        }
        return links;
    }

    public Set<Page> getSinglePages() throws SQLException {
        Set<Page> pages = new HashSet<>();
        ResultSet rs = getPagesWithCounts();
        while (rs.next()) {
            if (rs.getInt(COUNT_COLUMN) == 1) {
                    pages.add(new ModifiablePage(rs.getInt(ID_COLUMN),
                            rs.getString(PAGES_URL_COLUMN),
                            rs.getInt(PAGES_SITE_ID_COLUMN),
                            rs.getTimestamp(PAGES_FOUND_DATE_TIME_COLUMN),
                            rs.getTimestamp(PAGES_LAST_SCAN_DATE_COLUMN)));
            }
        }
        rs.close();
        return pages;
    }

    public Set<Page> getUnscannedSinglePages() throws SQLException {
        Set<Page> pages = new HashSet<>();
        ResultSet rs = getUnscannedPagesWithCounts();
        while (rs.next()) {
            if (rs.getInt(COUNT_COLUMN) == 1) {
                pages.add(new ModifiablePage(rs.getInt(ID_COLUMN),
                        rs.getString(PAGES_URL_COLUMN),
                        rs.getInt(PAGES_SITE_ID_COLUMN),
                        rs.getTimestamp(PAGES_FOUND_DATE_TIME_COLUMN),
                        rs.getTimestamp(PAGES_LAST_SCAN_DATE_COLUMN)));
            }
        }
        rs.close();
        return pages;
    }

    /**
     * Get collection of sites with only one page from "Pages" table.
     * @return  Site ID's
     * @throws SQLException
     */
    public Set<Integer> getSitesWithSinglePageIds() throws SQLException {
        Set<Integer> sites = new HashSet<>();
        ResultSet rs = getPagesWithCounts();
        while (rs.next()) {
            if (rs.getInt("COUNT(*)") == 1) {
                sites.add(rs.getInt("siteID"));
            }
        }
        rs.close();
        return sites;
    }

    /**
     * Get collection of sites with more than one page in "Pages" table.
     * @return  Site ID's
     * @throws SQLException
     */
    public Set<Integer> getSitesWithMultiplePageIds() throws SQLException {
        Set<Integer> sites = new HashSet<>();
        ResultSet rs = getPagesWithCounts();
        while (rs.next()) {
            if (rs.getInt("COUNT(*)") > 1) {
                sites.add(rs.getInt("siteID"));
            }
        }
        rs.close();
        return sites;
    }

    /**
     * @param siteId
     * @return Arbitrary link to the site
     * @throws SQLException
     */
    public @Nullable String getArbitrarySiteLink(int siteId) throws SQLException {
        String result = null;
        PreparedStatement stmt = conn.prepareStatement(
                                    "SELECT URL FROM pages WHERE siteId = ?");
        stmt.setInt(1, siteId);
        ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                result = rs.getString("URL");
            }
        stmt.close();
        return result;
    }

    /**
     * Inserts new row in "Pages" table.
     *
     * @param url           URL of the page being inseted
     * @param siteId        ID of the page's site in DB
     * @param lastScanDate  Timestamp when page was scanned
     *                      last time or null, if never was
     * @throws SQLException
     */
    public void insertRowInPagesTable(String url, int siteId,
                                      @Nullable Timestamp lastScanDate) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO pages (URL, siteID, lastScanDate) VALUES (?, ?, ?)" );
        stmt.setString(1, url);
        stmt.setInt(2, siteId);
        stmt.setTimestamp(3, lastScanDate);
        stmt.execute();
        stmt.close();
    }

    public void insertRowsInPagesTable(Set<String> urls, int siteId,
                                          @Nullable Timestamp lastScanDate) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(
                "INSERT INTO pages (URL, siteID, lastScanDate) VALUES (?, ?, ?)" );
        pst.setInt(2, siteId);
        pst.setTimestamp(3, lastScanDate);
        for (String url : urls) {
            pst.setString(1, url);
            pst.execute();
        }
        pst.close();
    }

    public void insertPersonsPageRanks(int personId, Map<String, Integer> pageRanks) throws SQLException {
        Map<String, Integer> pages = mapLinksToPageIds(pageRanks.keySet());
        PreparedStatement pst = conn.prepareStatement(
                "INSERT INTO personspagerank (PersonID,PageID,RANK) VALUES (?,?,?)");
        pst.setInt(1, personId);
        for (String url : pages.keySet()) {
            pst.setInt(2, pages.get(url));
            pst.setInt(3, pageRanks.get(url));
            pst.execute();
        }
        pst.close();
    }

    private Map<String, Integer> mapLinksToPageIds(Set<String> links) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("SELECT ID FROM pages WHERE URL = ?");
        Map<String, Integer> pages = new HashMap<>();
        for (String url : links) {
            pst.setString(1, url);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                pages.put(url, rs.getInt(ID_COLUMN));
            }
        }
        pst.close();
        return pages;
    }

    /**
     * Gets lastScanDate timestamp from pages table
     * in a row, where page with <code>url</code>
     * is located.
     *
     * @param url link, which is present in db
     * @return    lastScanDate timestamp
     * @throws SQLException
     */
    public Timestamp getLastScanDate(String url) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(
                "SELECT lastScanDate FROM pages WHERE URL = ?");
        pst.setString(1, url);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getTimestamp(PAGES_LAST_SCAN_DATE_COLUMN);
        }
        return null;
    }

    public void updateLastScanDate(Integer pageId,
                                    @Nullable Timestamp lastScanDate) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("UPDATE pages SET lastScanDate = ? WHERE ID = ?");
        stmt.setTimestamp(1, lastScanDate);
            stmt.setInt(2, pageId);
            stmt.execute();
            stmt.close();
    }

    public void updateLastScanDate(String pageURL,
                                   @Nullable Timestamp lastScanDate) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("UPDATE pages SET lastScanDate = ? WHERE URL = ?");
        stmt.setTimestamp(1, lastScanDate);
        stmt.setString(2, pageURL);
        stmt.execute();
        stmt.close();
    }

    public void updateLastScanDates(Set<Integer> pageIds,
                                       @Nullable Timestamp lastScanDate) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("UPDATE pages SET lastScanDate = ? WHERE ID = ?");
        stmt.setTimestamp(1, lastScanDate);
        for (Integer id : pageIds) {
            stmt.setInt(2, id);
            stmt.execute();
        }
        stmt.close();
    }

    public void updateLastScanDatesByUrl(Set<String> pageUrls,
                                    @Nullable Timestamp lastScanDate) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("UPDATE pages SET lastScanDate = ? WHERE URL = ?");
        stmt.setTimestamp(1, lastScanDate);
        for (String url : pageUrls ) {
            stmt.setString(2, url);
            stmt.execute();
        }
        stmt.close();
    }

    /**
     * Reads all site IDs from the database
     *
     * @return All of the site IDs from the base
     * @throws SQLException
     */
    public Set<Integer> getSiteIds() throws SQLException {
        Set<Integer> siteIds = new HashSet<>();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT ID FROM sites");
        while (rs.next()) {
            siteIds.add(rs.getInt(ID_COLUMN));
        }
        stmt.close();
        return siteIds;
    }

    public Integer getSiteIdByLink(String link) throws SQLException {
        Integer result = null;
        PreparedStatement stmt = conn.prepareStatement("SELECT siteID FROM pages WHERE URL = ?");
        stmt.setString(1, link);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            result = rs.getInt(PAGES_SITE_ID_COLUMN);
        }
        return result;
    }
}
