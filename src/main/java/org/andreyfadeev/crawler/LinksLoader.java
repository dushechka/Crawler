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

public class LinksLoader {
    private static final String PROTOCOL_DELIMITER = "://";
    public static final String ROBOTS_TXT_APPENDIX = "/robots.txt";
    private static final String LOC_TAG = "loc";
    private static final String LASTMOD_TAG = "lastmod";
    private static final DateTimeZone DATE_TIME_ZONE = DateTimeZone.forOffsetHours(3);

    /**
     * Takes arbitrary link to the site and returns it's address
     * in form, like http://example.com, with given links protocol.
     *
     * @param link  Arbitrary link to the site
     * @return      Site's common address
     * @throws MalformedURLException if site link is malformed.
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

    public Set<String> getLinksFromRobotsTxt(String robotsTxtLink) throws IOException {
        try (InputStream robotsTxtStream = new URL(robotsTxtLink).openStream()) {
            RobotsTxt robotsTxt = RobotsTxt.read(robotsTxtStream);
            return new HashSet<>(robotsTxt.getSitemaps());
        }
    }

    public Map<String, Timestamp> getLinksFromSitemap(String sitemap) throws IOException {
        final String UNSPECIFIED = "unspecified";
        Map<String, Timestamp> links = new HashMap<>();
        String url = UNSPECIFIED;
        for (Element element : PageFetcher.fetchSitemapElements(sitemap)) {
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
