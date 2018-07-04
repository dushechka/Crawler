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
package org.andreyfadeev.crawler;

import com.panforge.robotstxt.RobotsTxt;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;

/**
 * Loads web-page urls from various sources.
 *
 * @author Andrey Fadeev
 */
public class LinksLoader {
    private static final String PROTOCOL_DELIMITER = "://";
    private static final String LOC_TAG = "loc";
    private static final String LASTMOD_TAG = "lastmod";
    private static final DateTimeZone DATE_TIME_ZONE = DateTimeZone.forOffsetHours(3);

    /**
     *
     * @param link  arbitrary site page url
     * @return      Website's addres in form,
     *              like http://example.com,
     *              with given link's protocol.
     * @throws MalformedURLException when link param is malformed
     */
    public static String getSiteAddress(String link) throws MalformedURLException {
        URL url = new URL(link);
        return url.getProtocol() + PROTOCOL_DELIMITER + url.getHost();
    }

    public static boolean isSiteAvailable(String address) throws IOException {
        URL url = new URL(address);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        boolean result = (connection.getResponseCode() == 200);
        connection.disconnect();
        return result;
    }

    /**
     * Fetches sitemap links from robots.txt file.
     *
     * @param robotsTxtLink url of the robots.txt file
     * @return              all sitemap links from robots.txt
     * @throws IOException
     */
    public Set<String> getLinksFromRobotsTxt(String robotsTxtLink) throws IOException {
        try (InputStream robotsTxtStream = new URL(robotsTxtLink).openStream()) {
            RobotsTxt robotsTxt = RobotsTxt.read(robotsTxtStream);
            return new HashSet<>(robotsTxt.getSitemaps());
        }
    }

    /**
     * Fetches links from sitemap xml file.
     * <p>
     *     Sitemap param file can be gzipped.
     * </p>
     *
     * @param sitemapLink   url of the sitemap file
     * @return              All of the links,
     *                      contained in the sitemap.
     * @throws IOException
     */
    public Map<String, Timestamp> getLinksFromSitemap(String sitemapLink) throws IOException {
        final String UNSPECIFIED = "unspecified";
        Map<String, Timestamp> links = new HashMap<>();
        PageFetcher pf = new PageFetcher();
        String url = UNSPECIFIED;
        for (Element element : pf.fetchSitemapElements(sitemapLink)) {
            Elements elts = element.getAllElements();
            for (Element elt : elts) {
                if (elt.tagName().equals(LOC_TAG)) {
                    url = elt.text();
                    links.put(url, null);
                }
                if (elt.tagName().equals(LASTMOD_TAG)) {
                    DateTime lastmod = DateTime.parse(elt.text());
                    lastmod = lastmod.withZone(DATE_TIME_ZONE);
                    links.put(url, new Timestamp(lastmod.getMillis()));
                }
            }
        }
        links.remove(UNSPECIFIED);
        return links;
    }
}
