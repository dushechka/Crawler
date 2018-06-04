package com.allendowney.thinkdast;

import com.allendowney.thinkdast.interfaces.SitemapLoader;

import java.net.URL;
import java.util.Set;

public class LinksLoader implements SitemapLoader {
    @Override
    public Set<String> getPagesFromRobotsTxt(String robotsTxtLink) {
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
