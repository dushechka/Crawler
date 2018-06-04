package dbs.sql;

import com.sun.istack.internal.Nullable;
import dbs.sql.orm.ModifiablePage;
import dbs.sql.orm.Page;

import java.net.URL;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class RatesDatabase {

    private static final String PAGES_ID_COLUMN = "ID";
    private static final String PAGES_URL_COLUMN = "URL";
    private static final String PAGES_SITE_ID_COLUMN = "siteID";
    private static final String PAGES_FOUND_DATE_TIME_COLUMN = "foundDateTime";
    private static final String PAGES_LAST_SCAN_DATE_COLUMN = "lastScanDate";
    private static final String COUNT_COLUMN = "COUNT(*)";
    private final Connection conn;

    public RatesDatabase(Connection conn) {
        this.conn = conn;
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
     * Gets unscanned links to robots.txt from database.
     *
     * @return Unscanned links to robots.txt files or null if none.
     * @throws SQLException
     */
    public @Nullable Set<String> getUnscannedRobotsTxtLinks() throws SQLException {
        Set<String> links = new HashSet<>();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "SELECT URL FROM pages WHERE URL LIKE '%robots.txt' AND lastScanDate IS NULL");
        while (rs.next()) {
            links.add(rs.getString("URL"));
        }
        if (links.size() > 0) {
            return links;
        } else {
            return null;
        }
    }

    public Set<Page> getSinglePages() throws SQLException {
        Set<Page> pages = new HashSet<>();
        ResultSet rs = getPagesWithCounts();
        while (rs.next()) {
            if (rs.getInt(COUNT_COLUMN) == 1) {
                    pages.add(new ModifiablePage(rs.getInt(PAGES_ID_COLUMN),
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
    public Set<Integer> getSitesWithSinglePagesIds() throws SQLException {
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
    public Set<Integer> getSitesWithMultiplePagesIds() throws SQLException {
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
    public void insertRowInPagesTable(URL url, int siteId,
                                      @Nullable Timestamp lastScanDate) throws SQLException {
            // checking, if url is malformed
            System.out.println("Creating URL from: " + url);
            PreparedStatement stmt = conn.prepareStatement(
                                        "INSERT INTO pages (URL, siteID, lastScanDate) VALUES (?, ?, ?)" );
            stmt.setString(1, url.toString());
            stmt.setInt(2, siteId);
            stmt.setTimestamp(3, lastScanDate);
            stmt.execute();
            stmt.close();
    }

    public void updateLastScanDate(Integer pageId,
                                    @Nullable Timestamp lastScanDate) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("UPDATE pages SET lastScanDate = ? WHERE siteId = ?");
        stmt.setTimestamp(1, lastScanDate);
            stmt.setInt(2, pageId);
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
}
