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

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Index {
    /**
     * Checks whether we have a TermCounter for a given URL.
     *
     * @param url
     * @return
     */
    boolean isIndexed(String url);

    /**
     * Adds a URL to the set associated with `term`.
     *
     * @param term
     * @param url
     */
    void add(String term, String url);

    /**
     * Looks up a search term and returns a set of URLs.
     *
     * @param term
     * @return Set of URLs.
     */
    Set<String> getURLs(String term);

    /**
     * Looks up a term and returns a map from URL to count.
     *
     * @param term
     * @return Map from URL to count.
     */
    Map<String, Integer> getCounts(String term);

    /**
     * Returns the number of times the given term appears at the given URL.
     *
     * @param url
     * @param term
     * @return null if no such enty in the index
     */
    @Nullable Integer getCount(String url, String term);

    /**
     * Adds vocabulary from page to the index.
     *
     * @param tc
     * @return List of terms indexed.
     */
    List<String> putTerms(TermContainer tc);

    /**
     * Returns the set of terms that have been indexed.
     *
     * Should be used for development and testing, not production.
     *
     * @return
     */
    Set<String> termSet();

    /**
     * Prints the contents of the index.
     *
     * Should be used for development and testing, not production.
     */
    void printIndex();

    void close();
}
