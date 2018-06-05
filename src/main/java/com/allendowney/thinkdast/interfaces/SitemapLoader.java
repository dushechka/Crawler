package com.allendowney.thinkdast.interfaces;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface SitemapLoader {
    Map<String, Set<String>> getPagesFromRobotsTxt(String robotsTxtLink) throws IOException;
    Map<String, Set<String>> getPagesFromSitemap(String sitemapLink);
    Map<String, Set<String>> getPagesFromSitemaps(Set<String> sitemapLinks);
}
