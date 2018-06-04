package com.allendowney.thinkdast.interfaces;

import java.io.IOException;
import java.util.Set;

public interface SitemapLoader {
    Set<String> getPagesFromRobotsTxt(String robotsTxtLink) throws IOException;
    Set<String> getPagesFromSitemap(String sitemapLink);
    Set<String> getPagesFromSitemaps(Set<String> sitemapLinks);
}
