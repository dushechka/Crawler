package com.allendowney.thinkdast.interfaces;

import java.util.Set;

public interface SitemapLoader {
    Set<String> getPagesLinksFromRobotsTxt(String robotsTxtLink);
    Set<String> getPagesLinksFromSitemap(String sitemapLink);
    Set<String> getPagesLinksFromSitemaps(Set<String> sitemapLinks);
}
