package com.allendowney.thinkdast;

import com.allendowney.thinkdast.interfaces.SitemapLoader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

public class LinksLoader implements SitemapLoader {
    private static final String PROTOCOL_DELIMITER = "://";
    public static final String ROBOTS_TXT_APPENDIX = "/robots.txt";

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
        boolean result = false;
        URL url = new URL(address);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        result = (connection.getResponseCode() == 200);
        connection.disconnect();
        return result;
    }

    @Override
    public Set<String> getPagesFromRobotsTxt(String robotsTxtLink) {
        PageFetcher pf = new PageFetcher();
        try {
            System.out.println(pf.getTxtFileContent(robotsTxtLink));
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        return null;
    }

    @Override
    public Set<String> getPagesFromSitemap(String sitemapLink) {
        return null;
    }

    @Override
    public Set<String> getPagesFromSitemaps(Set<String> sitemapLinks) {
        return null;
    }

    private Set<String> getPagesFromSitemapWithUrls(URL sitemap) {
        return null;
    }
}
