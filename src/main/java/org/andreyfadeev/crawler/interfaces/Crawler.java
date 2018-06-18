package org.andreyfadeev.crawler.interfaces;

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
     * @return pages, that was unable to index
     */
    Set<String> crawlPages(Set<String> links, Index index) throws Exception;

    /**
     * Indexes given url.
     *
     * @return	URL of the page indexed.
     * @throws IOException
     */
    String crawlPage(String url, Index index) throws Exception;
}
