package dbs.sql;

import com.sun.istack.internal.Nullable;
import dbs.sql.orm.ModifiablePage;
import dbs.sql.orm.Page;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class RatesDatabase {
    private static final String PROTOCOL_DELIMITER = "://";
    private static final String PAGES_ID_COLUMN = "ID";
    private static final String PAGES_URL_COLUMN = "URL";
    private static final String PAGES_SITE_ID_COLUMN = "siteID";
    private static final String PAGES_FOUND_DATE_TIME_COLUMN = "foundDateTime";
    private static final String PAGES_LAST_SCAN_DATE_COLUMNT = "lastScanDate";
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

    public Set<Page> getSinglePages() throws SQLException {
        Set<Page> pages = new HashSet<>();
        Set<Integer> malformedUrls = new HashSet<>();
        ResultSet rs = getPagesWithCounts();
        while (rs.next()) {
            if (rs.getInt(COUNT_COLUMN) == 1
                        && rs.getTimestamp(PAGES_LAST_SCAN_DATE_COLUMNT) == null) {
                    pages.add(new ModifiablePage(rs.getInt(PAGES_ID_COLUMN),
                            rs.getString(PAGES_URL_COLUMN),
                            rs.getInt(PAGES_SITE_ID_COLUMN),
                            rs.getTimestamp(PAGES_FOUND_DATE_TIME_COLUMN),
                            rs.getTimestamp(PAGES_LAST_SCAN_DATE_COLUMNT)));
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
    public Set<Integer> getSitesWithSinglePages() throws SQLException {
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
    public Set<Integer> getSitesWithMultiplePages() throws SQLException {
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
     * Searches for a link to the given <code>siteId</code>
     * in database and returns address to this site in form:
     * "protocol://host.domain". For example: "https://example.com".
     * @param siteId
     * @return Site address or null, if there's no such
     *         <code>siteId</code> or link for this site
     *         in database is malformed.
     * @throws SQLException
     */
    public @Nullable String getSiteAddress(int siteId) throws SQLException {
        String result = null;
        PreparedStatement stmt = conn.prepareStatement(
                                    "SELECT URL FROM pages WHERE siteId = ?");
        stmt.setInt(1, siteId);
        ResultSet rs = stmt.executeQuery();
        try {
            if (rs.next()) {
                URL url = new URL(rs.getString("URL"));
                result = url.getProtocol() + PROTOCOL_DELIMITER + url.getHost();
            }
        } catch (MalformedURLException exc) {
            System.out.println("Malformed URL: siteID = " + siteId);
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
    public void insertRowInPages(URL url, int siteId,
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
}
