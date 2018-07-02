/*
 * Copyright (c) 2018 Andrey Fadeev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    Set<TermContainer> crawlPages(Set<String> links, Index index) throws Exception;

    /**
     * Indexes given url.
     *
     * @param url   url to be indexed
     * @param index index to save vocabulary from page
     * @return	URL of the page indexed.
     * @throws IOException
     */
    TermContainer crawlPage(String url, Index index) throws Exception;
}
