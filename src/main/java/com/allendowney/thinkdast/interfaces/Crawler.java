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
     * Crawl all of the given links and indexes them.
     * @param links
     * @return Crawled pages URLs.
     */
    Set<String> crawlPages(Set<String> links);

    /**
     * Indexes given url.
     *
     * @return	URL of the page indexed.
     * @throws IOException
     */
    String crawlPage(String url) throws IOException;
}
