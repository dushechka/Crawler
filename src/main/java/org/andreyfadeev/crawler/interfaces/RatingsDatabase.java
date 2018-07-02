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
package org.andreyfadeev.crawler.interfaces;

import org.andreyfadeev.crawler.dbs.sql.orm.Page;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;

/**
 * Interface for working with database,
 * containing ranks for persons on websites.
 *
 * @author Andrey Fadeev
 */
public interface RatingsDatabase {

    /**
     *
     * @param link  link to the site, presented in db
     * @return
     * @throws SQLException
     */
    Integer getSiteId(String link) throws SQLException;

    /**
     *
     * @return  all of the site ids in the database
     * @throws SQLException
     */
    Set<Integer> getSiteIds() throws SQLException;

    /**
     * @param siteId
     * @return  null, if no links for site is presented in db
     * @throws SQLException
     */
    @Nullable
    String getArbitrarySiteLink(int siteId) throws SQLException;

    /**
     *
     * @return  person ids, mapped to link-rank maps
     * @throws SQLException
     */
    Map<Integer, Map<String, Integer>> getPersonsPageRanks() throws SQLException;

    /**
     *
     * @return  person ids, mapped to sets of their keywords
     * @throws SQLException
     */
    Map<Integer, Set<String>> getPersonsWithKeywords() throws SQLException;

    /**
     *
     * @param url link, which is present in db
     * @return    lastScanDate timestamp
     * @throws SQLException if there is no such link in db
     */
    @Nullable Timestamp getLastScanDate(String url) throws SQLException;

    /**
     *
     * @return  A set of <code>Page</code> objects
     *          for sites, which have only one page in db.
     * @throws SQLException
     */
    Set<Page> getSinglePages() throws SQLException;

    /**
     *
     * @return  A set of <code>Page</code> objects
     *          for sites, which have only one page
     *          in db, and never been scanned.
     * @throws SQLException
     */
    Set<Page> getUnscannedSinglePages() throws SQLException;

    /**
     *
     * @param siteId    id of the site for which to get links
     * @param limit     maximum amount of links to extract
     * @return  leaf pages links, which never been scanned
     * @throws SQLException
     */
    Set<String> getBunchOfUnscannedLinks(int siteId, int limit) throws SQLException;

    /**
     *
     * @param siteId    id of the site for which to get pages
     * @param limit     maximum amount of pages to extract
     * @return  A set of <code>Page</code>
     *          for pages, which never been scanned.
     * @throws SQLException
     */
    Set<Page> getBunchOfUnscannedPages(int siteId, int limit) throws SQLException;

    /**
     *
     * @return  unscanned links to robots.txt files
     * @throws SQLException
     */
    Set<String> getUnscannedRobotsTxtLinks() throws SQLException;

    /**
     *
     * @return  unscanned links to sitemap.xml files
     * @throws SQLException
     */
    Set<String> getUnscannedSitemapLinks() throws SQLException;

    /**
     *
     * @param siteId    id of the site, for which to get links
     * @return  unscanned links to sitemap.xml files
     * @throws SQLException
     */
    Set<String> getUnscannedSitemapLinks(int siteId) throws SQLException;

    /**
     * @return  set of ids for sites, with only one page in db
     * @throws SQLException
     */
    Set<Integer> getSitesWithSinglePageIds() throws SQLException;

    /**
     * @return  Set of ids for sites, which
     *          have more than on page in db.
     * @throws SQLException
     */
    Set<Integer> getSitesWithMultiplePageIds() throws SQLException;

    /**
     * Inserts new row in "Pages" table.
     *
     * @param url           link to the page being inserted
     * @param siteId        id of the page's site in db
     * @param lastScanDate  Timestamp when page was scanned
     *                      last time or null, if never was.
     * @throws SQLException
     */
    void insertRowInPagesTable(String url, int siteId,
                               @Nullable Timestamp lastScanDate) throws SQLException;

    /**
     * Inserts new rows in "Pages" table.
     *
     * @param urls          links to the pages, being inserted
     * @param siteId        id of the page's site in db
     * @param lastScanDate  Timestamp when page was scanned
     *                      last time or null, if never was.
     * @throws SQLException
     */
    void insertRowsInPagesTable(Set<String> urls, int siteId,
                                @Nullable Timestamp lastScanDate) throws SQLException;

    /**
     * Inserts new rows in "Pages" table.
     *
     * @param pages     page urls with page creation timestamps
     * @param siteId
     * @param lastScanDate
     * @throws SQLException
     */
    void insertRowsInPagesTable(Map<String, Timestamp> pages, int siteId,
                                @Nullable Timestamp lastScanDate) throws SQLException;

    /**
     * Inserts new rows in "PersonsPageRank" table.
     *
     * @param personId  for whom rows are being inserted
     * @param pageRanks a map of link-rank entries for person
     * @throws SQLException
     */
    void insertPersonPageRanks(int personId, Map<String, Integer> pageRanks) throws SQLException;

    /**
     *
     * @param url           link to the page
     * @param foundDateTime timestamp of the page creation
     * @throws SQLException
     */
    void updateFoundDateTime(String url, @Nullable Timestamp foundDateTime) throws SQLException;

    /**
     *
     * @param personId
     * @param link  url of the page, containing person keywords
     * @param rank  amount of keywords occurrences in page article
     * @throws SQLException
     */
    void updatePersonPageRank(int personId, String link, Integer rank) throws SQLException;

    void updateLastScanDate(Integer pageId,
                            @Nullable Timestamp lastScanDate) throws SQLException;

    void updateLastScanDate(String pageURL,
                            @Nullable Timestamp lastScanDate) throws SQLException;

    /**
     *
     * @param pageIds       ids for pages in db
     * @param lastScanDate  Timestamp, when page was
     *                      scanned last time.
     * @throws SQLException
     */
    void updateLastScanDates(Set<Integer> pageIds,
                             @Nullable Timestamp lastScanDate) throws SQLException;

    /**
     * Updates last scan timestamps
     * for given pages in database.
     *
     * @param pageUrls
     * @param lastScanDate
     * @throws SQLException
     */
    void updateLastScanDatesByUrl(Set<String> pageUrls,
                                  @Nullable Timestamp lastScanDate) throws SQLException;

    /**
     * Closes database connection
     * for this object.
     * <p>
     *     Further work with database through
     *     this object, after this method
     *     invocation, is impossible.
     * </p>
     *
     * @throws SQLException
     */
    void close() throws SQLException;
}
