package dbs.sql;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class RatesDatabase {
    public static final String PROTOCOL_DELIMITER = "://";
    private final Connection conn;

    public RatesDatabase(Connection conn) {
        this.conn = conn;
    }

    private ResultSet getPageCounts() throws SQLException {
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(
                "SELECT siteID, COUNT(*) FROM pages GROUP BY siteID;");
    }

    public Set<Integer> getSitesWithSinglePages() throws SQLException {
        Set<Integer> sites = new HashSet<>();
        ResultSet rs = getPageCounts();
        while (rs.next()) {
            if (rs.getInt("COUNT(*)") == 1) {
                sites.add(rs.getInt("siteID"));
            }
        }
        rs.close();
        return sites;
    }

    public Set<Integer> getSitesWithMultiplePages() throws SQLException {
        Set<Integer> sites = new HashSet<>();
        ResultSet rs = getPageCounts();
        while (rs.next()) {
            if (rs.getInt("COUNT(*)") > 1) {
                sites.add(rs.getInt("siteID"));
            }
        }
        rs.close();
        return sites;
    }

    public String getSiteAddress(int siteId) throws SQLException {
        String result = null;
        PreparedStatement stmt = conn.prepareStatement(
                                    "SELECT URL FROM pages WHERE siteId = ?;");
        stmt.setInt(1, siteId);
        ResultSet rs = stmt.executeQuery();
        try {
            if (rs.next()) {
                URL url = new URL(rs.getString("URL"));
                result = url.getProtocol() + PROTOCOL_DELIMITER + url.getHost();
            }
        } catch (MalformedURLException exc) {
            exc.printStackTrace();
        }
        stmt.close();
        return result;
    }
}
