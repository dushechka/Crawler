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
