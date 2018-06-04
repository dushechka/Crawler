package com.allendowney.thinkdast.interfaces;

import java.util.Set;

public interface SitemapLoader {
    Set<String> getPagesFromRobotsTxt(String robotsTxtLink);
    Set<String> getPagesFromSitemap(String sitemapLink);
    Set<String> getPagesFromSitemaps(Set<String> sitemapLinks);
}
