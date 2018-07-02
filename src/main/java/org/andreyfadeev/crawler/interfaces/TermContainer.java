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

import java.util.Set;

public interface TermContainer {
    String getLabel();

    /**
     * Returns the total of all counts.
     *
     * @return
     */
    int size();

    /**
     * Increments the counter associated with `term`.
     *
     * @param term
     */
    void incrementTermCount(String term);

    /**
     * Adds a term to the map with a given count.
     *
     * @param term
     * @param count
     */
    void put(String term, int count);

    /**
     * Returns the count associated with this term, or 0 if it is unseen.
     *
     * @param term
     * @return
     */
    Integer get(String term);

    /**
     * Returns the set of terms that have been counted.
     *
     * @return
     */
    Set<String> keySet();
}
