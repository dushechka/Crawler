package dbs.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class RatesDatabase {
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
}
