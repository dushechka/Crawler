/*
 * Copyright (c) 2018 Andrey Fadeev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andreyfadeev.crawler.dbs.sql;

import org.andreyfadeev.crawler.interfaces.RatingsDatabase;
import org.andreyfadeev.crawler.dbs.sql.orm.ModifiablePage;
import org.andreyfadeev.crawler.dbs.sql.orm.Page;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * <code>RatingsDatabase</code> interface
 * implementation for working with MySQL db.
 *
 * @author Andrey Fadeev
 */
public class MySqlRatingsDatabase implements RatingsDatabase {
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
    private static final String PERSON_ID_COLUMN = "PersonID";
    private static final String PAGE_ID_COLUMN = "PageID";
    private static final String RANK_COLUMN = "Rank";
    private final Connection conn;

    public MySqlRatingsDatabase(Connection conn) {
        this.conn = conn;
    }

    private ResultSet getPersons() throws SQLException {
        Statement stmt = conn.createStatement();
        return stmt.executeQuery("SELECT * FROM persons");
    }

    @Override
    public Map<Integer, Map<String, Integer>> getPersonsPageRanks() throws SQLException {
        Map<Integer, Map<String, Integer>> personsPageRanks = new HashMap<>();
        ResultSet rs = getPersons();
        while (rs.next()) {
            personsPageRanks.put(rs.getInt(ID_COLUMN), new HashMap<>());
        }
        rs.close();
        Statement stmt = conn.createStatement();
        PreparedStatement pst = conn.prepareStatement("SELECT URL FROM pages WHERE ID = ?");
        rs = stmt.executeQuery("SELECT * FROM personspagerank");
        while (rs.next()) {
            pst.setInt(1, rs.getInt(PAGE_ID_COLUMN));
            ResultSet rset = pst.executeQuery();
            if (rset.next()) {
                int personId = rs.getInt(PERSON_ID_COLUMN);
                Map<String, Integer> ppr = personsPageRanks.get(personId);
                ppr.put(rset.getString(PAGES_URL_COLUMN), rs.getInt(RANK_COLUMN));
            }
        }
        stmt.close();
        pst.close();
        return personsPageRanks;
    }

    @Override
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

    private ResultSet getPagesWithCounts() throws SQLException {
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(
                "SELECT *, COUNT(*) FROM pages GROUP BY siteID");
    }

    private ResultSet getUnscannedPagesWithCounts() throws SQLException {
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(
                "SELECT *, COUNT(*) FROM pages WHERE lastScanDate IS NULL GROUP BY siteID");
    }

    @Override
    public Set<String> getBunchOfUnscannedLinks(int siteId, int limit) throws SQLException {
        Set<String> links = new HashSet<>();
        PreparedStatement pst = conn.prepareStatement(
                "SELECT URL FROM pages WHERE siteID = ? AND lastScanDate IS NULL " +
                                                "AND URL NOT LIKE '%sitemap%xml' LIMIT ?");
        pst.setInt(1, siteId);
        pst.setInt(2, limit);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            String link = rs.getString(PAGES_URL_COLUMN);
            System.out.println(link);
            if (!(link.contains(ROBOTS_TXT_APPENDIX))) {
                links.add(link);
            }
        }
        pst.close();
        return links;
    }

    @Override
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

    @Override
    public Set<String> getUnscannedRobotsTxtLinks() throws SQLException {
        Set<String> links = new HashSet<>();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "SELECT URL FROM pages WHERE URL LIKE '%robots.txt' AND lastScanDate IS NULL");
        while (rs.next()) {
            links.add(rs.getString("URL"));
        }
        return links;
    }

    @Override
    public Set<String> getUnscannedSitemapLinks() throws SQLException {
        Set<String> links = new HashSet<>();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "SELECT URL FROM pages WHERE URL LIKE '%sitemap%xml%' AND lastScanDate IS NULL");
        while (rs.next()) {
            links.add(rs.getString("URL"));
        }
        stmt.close();
        return links;
    }

    @Override
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

    @Override
    public Set<String> getUnscannedSitemapLinks(int siteId) throws SQLException {
        Set<String> links = new HashSet<>();
        PreparedStatement pst = conn.prepareStatement(
                "SELECT URL FROM pages WHERE URL LIKE '%sitemap%xml%' AND lastScanDate IS NULL AND siteID = ?");
        pst.setInt(1, siteId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            links.add(rs.getString("URL"));
        }
        pst.close();
        return links;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void insertRowInPagesTable(String url, int siteId,
                                      @Nullable Timestamp lastScanDate) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO pages (URL, siteID, lastScanDate) VALUES (?, ?, ?)" );
        stmt.setString(1, url);
        stmt.setInt(2, siteId);
        stmt.setTimestamp(3, lastScanDate);
        try {
            stmt.execute();
        } catch (SQLIntegrityConstraintViolationException exc) {
            exc.printStackTrace();
        }
        stmt.close();
    }

    @Override
    public void insertRowsInPagesTable(Set<String> urls, int siteId,
                                       @Nullable Timestamp lastScanDate) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(
                "INSERT INTO pages (URL, siteID, lastScanDate) VALUES (?, ?, ?)" );
        pst.setInt(2, siteId);
        pst.setTimestamp(3, lastScanDate);
        for (String url : urls) {
            try {
                pst.setString(1, url);
                pst.execute();
            } catch (SQLIntegrityConstraintViolationException exc) {
                exc.printStackTrace();
            }
        }
        pst.close();
    }

    @Override
    public void insertRowsInPagesTable(Map<String, Timestamp> pages, int siteId,
                                       @Nullable Timestamp lastScanDate) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(
                "INSERT INTO pages (URL, siteID, foundDateTime, lastScanDate) VALUES (?, ?, ?, ?)");
        pst.setInt(2, siteId);
        pst.setTimestamp(4, lastScanDate);
        for (String url : pages.keySet()) {
            Timestamp foundDateTime = pages.get(url);
            try {
                pst.setString(1, url);
                pst.setTimestamp(3, foundDateTime);
                pst.execute();
            } catch (SQLIntegrityConstraintViolationException exc) {
                exc.printStackTrace();
            }
        }
        pst.close();
    }

    @Override
    public void updateFoundDateTime(String url, @Nullable Timestamp foundDateTime) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(
                "UPDATE pages SET foundDateTime = ? WHERE URL = ?");
        pst.setTimestamp(1, foundDateTime);
        pst.setString(2, url);
        pst.execute();
        pst.close();
    }

    @Override
    public void updatePersonPageRank(int personId, String link, Integer rank) throws SQLException{
        Integer pageId = getPageId(link);
        if (pageId != null) {
            PreparedStatement pst = conn.prepareStatement(
                    "UPDATE personspagerank SET Rank = ? WHERE PersonID = ? and PageID = ?");
            pst.setInt(1, rank);
            pst.setInt(2, personId);
            pst.setInt(3, pageId);
            pst.execute();
            pst.close();
        }
    }

    @Override
    public void insertPersonPageRanks(int personId, Map<String, Integer> pageRanks) throws SQLException {
        Map<String, Integer> pages = mapLinksToPageIds(pageRanks.keySet());
        PreparedStatement pst = conn.prepareStatement(
                "INSERT INTO personspagerank (PersonID,PageID,RANK) VALUES (?,?,?)");
        pst.setInt(1, personId);
        for (String url : pages.keySet()) {
            try {
                pst.setInt(2, pages.get(url));
                pst.setInt(3, pageRanks.get(url));
                pst.execute();
            } catch (SQLIntegrityConstraintViolationException exc) {
                exc.printStackTrace();
            }
        }
        pst.close();
    }

    private @Nullable Integer getPageId(String link) throws SQLException {
        Integer pageId = null;
        PreparedStatement pst = conn.prepareStatement("SELECT ID FROM pages WHERE URL = ?");
        pst.setString(1, link);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            pageId = rs.getInt(ID_COLUMN);
        }
        pst.close();
        return pageId;
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

    @Override
    public @Nullable Timestamp getLastScanDate(String url) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(
                "SELECT lastScanDate FROM pages WHERE URL = ?");
        pst.setString(1, url);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getTimestamp(PAGES_LAST_SCAN_DATE_COLUMN);
        }
        throw new SQLException("No such link in db!");
    }

    @Override
    public void updateLastScanDate(Integer pageId,
                                   @Nullable Timestamp lastScanDate) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("UPDATE pages SET lastScanDate = ? WHERE ID = ?");
        stmt.setTimestamp(1, lastScanDate);
            stmt.setInt(2, pageId);
            stmt.execute();
            stmt.close();
    }

    @Override
    public void updateLastScanDate(String pageURL,
                                   @Nullable Timestamp lastScanDate) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("UPDATE pages SET lastScanDate = ? WHERE URL = ?");
        stmt.setTimestamp(1, lastScanDate);
        stmt.setString(2, pageURL);
        stmt.execute();
        stmt.close();
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Integer getSiteId(String link) throws SQLException {
        Integer result = null;
        PreparedStatement stmt = conn.prepareStatement("SELECT siteID FROM pages WHERE URL = ?");
        stmt.setString(1, link);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            result = rs.getInt(PAGES_SITE_ID_COLUMN);
        }
        return result;
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }
}
