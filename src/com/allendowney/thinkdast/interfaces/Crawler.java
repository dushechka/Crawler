package com.allendowney.thinkdast.interfaces;

import java.io.IOException;
import java.util.Set;

public interface Crawler {
    /**
     * Returns the number of URLs in the queue.
     *
     * @return
     */
    int queueSize();

    /**
     * Gets site's pages by given URL.
     * @param url
     * @return All site's pages.
     * @return
     */
    Set<String> getSitePages(String url);

    /**
     * Crawls all of the site's links and indexes them.
     * @param url
* @return Crawled pages URLs.
     */
    Set<String> crawl(String url);

    /**
     * Crawl all of the given links and indexes them.
     * @param links
     * @return Crawled pages URLs.
     */
    Set<String> crawl(Set<String> links);

    /**
     * Indexes given url.
     *
     * @return	URL of the page indexed.
     * @throws IOException
     */
    String crawlPage(String url) throws IOException;
}
