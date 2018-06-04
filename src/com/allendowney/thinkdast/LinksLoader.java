package com.allendowney.thinkdast;

import com.allendowney.thinkdast.interfaces.SitemapLoader;
import com.sun.istack.internal.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
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
    public @Nullable Set<String> getPagesLinksFromRobotsTxt(String robotsTxtLink) {
        try {
            Set<String> rootSitemaps = getRootSiteMaps(robotsTxtLink);
            System.out.println("Printin' fetched sitemaps:");
            for (String link : rootSitemaps) {
                System.out.println(link);
            }
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        return null;
    }

    private Set<String> getRootSiteMaps(String link) throws IOException {
        Set<String> sitemaps = new HashSet<>();
        URL url = new URL(link);
        BufferedReader br = new BufferedReader(
                                    new InputStreamReader(url.openStream()));
        String line;
        while (br.ready()) {
            line = br.readLine().toLowerCase().trim();
            if (line.startsWith("sitemap")) {
                sitemaps.add(line.substring(line.indexOf("http")));
            }
        }
        br.close();
        return sitemaps;
    }

    @Override
    public Set<String> getPagesLinksFromSitemap(String sitemapLink) {
        return null;
    }

    @Override
    public Set<String> getPagesLinksFromSitemaps(Set<String> sitemapLinks) {
        return null;
    }

    private Set<String> getLinksFromSitemapWithUrls(URL sitemap) {
        return null;
    }
}
