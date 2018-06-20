package org.andreyfadeev.crawler.dbs.redis;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.andreyfadeev.crawler.interfaces.Index;
import org.andreyfadeev.crawler.interfaces.TermContainer;

import java.util.*;

public class LettuceIndex implements Index {


    private static final String COLON = ":";
    private static final String CRAWLER_PREFIX = "Crawler_";
    private static final String URLSET_PREFIX = "URLSet:";
    private static final String TERM_COUNTER_PREFIX = "TermCounter:";
    private static final String ASTERISK = "*";
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> syncCommands;

    public LettuceIndex(StatefulRedisConnection conn) {
        this.connection = conn;
        syncCommands = conn.sync();
    }

    /**
     * Returns the Redis key for a given search term.
     *
     * @return Redis key.
     */
    private String urlSetKey(String term) {
        return CRAWLER_PREFIX + URLSET_PREFIX + term;
    }

    /**
     * Returns the Redis key for a URL's TermCounter.
     *
     * @return Redis key.
     */
    private String termCounterKey(String url) {
        return CRAWLER_PREFIX + TERM_COUNTER_PREFIX + url;
    }

    /**
     * Checks whether we have a TermCounter for a given URL.
     *
     * @param url
     * @return
     */
    @Override
    public boolean isIndexed(String url) {
        String redisKey = termCounterKey(url);
        return (syncCommands.exists(redisKey) > 0);
    }

    /**
     * Adds a URL to the set associated with `term`.
     *
     * @param term
     * @param url
     */
    @Override
    public void add(String term, String url) {
        syncCommands.sadd(urlSetKey(term), url);
    }

    /**
     * Looks up a search term and returns a set of URLs.
     *
     * @param term
     * @return Set of URLs.
     */
    @Override
    public Set<String> getURLs(String term) {
        return syncCommands.smembers(urlSetKey(term));
    }

    /**
     * Looks up a term and returns a map from URL to count.
     *
     * @param term
     * @return Map from URL to count.
     */
    @Override
    public Map<String, Integer> getCounts(String term) {
        Map<String, Integer> map = new HashMap<>();
        Set<String> urls = getURLs(term);
        for (String url: urls) {
            Integer count = getCount(url, term);
            map.put(url, count);
        }
        return map;
    }

    /**
     * Returns the number of times the given term appears at the given URL.
     *
     * @param url
     * @param term
     * @return
     */
    @Override
    public Integer getCount(String url, String term) {
        String redisKey = termCounterKey(url);
        String count = syncCommands.hget(redisKey, term);
        return new Integer(count);
    }

    /**
     * Adds vocabulary of page to the index.
     *
     * @param tc
     * @return List of return values from Redis.
     */
    @Override
    public List<String> putTerms(TermContainer tc) {
        List<String> terms = new ArrayList<>();
        System.out.println("Putting terms in index: ");
        System.out.println(tc);

        String url = tc.getLabel();
        String hashName = termCounterKey(url);

        syncCommands.del(hashName);

        for (String term: tc.keySet()) {
            Integer count = tc.get(term);
            syncCommands.hset(hashName, term, count.toString());
            syncCommands.sadd(urlSetKey(term), url);
            terms.add(term);
        }

        return terms;
    }

    /**
     * Prints the contents of the index.
     *
     * Should be used for development and testing, not production.
     */
    public void printIndex() {
        // loop through the search terms
        for (String term: termSet()) {
            System.out.println(term);

            // for each term, print the pages where it appears
            Set<String> urls = getURLs(term);
            for (String url: urls) {
                Integer count = getCount(url, term);
                System.out.println("    " + url + " " + count);
            }
        }
    }

    /**
     * Returns the set of terms that have been indexed.
     *
     * Should be used for development and testing, not production.
     *
     * @return
     */
    @Override
    public Set<String> termSet() {
        Set<String> keys = urlSetKeys();
        Set<String> terms = new HashSet<>();
        for (String key: keys) {
            String[] array = key.split(COLON);
            if (array.length < 2) {
                terms.add("");
            } else {
                terms.add(array[1]);
            }
        }
        return terms;
    }

    /**
     * Returns URLSet keys for the terms that have been indexed.
     *
     * Should be used for development and testing, not production.
     *
     * @return
     */
    public Set<String> urlSetKeys() {
        return new HashSet<>(syncCommands.keys(urlSetKey(ASTERISK)));
    }

    /**
     * Returns TermCounter keys for the URLS that have been indexed.
     *
     * Should be used for development and testing, not production.
     *
     * @return
     */
    public Set<String> termCounterKeys() {
        return new HashSet<>(syncCommands.keys(termCounterKey(ASTERISK)));
    }

    @Override
    public void close() {
        connection.close();
    }
}
