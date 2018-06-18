package org.andreyfadeev.crawler.dbs.redis;

import org.andreyfadeev.crawler.interfaces.Index;
import org.andreyfadeev.crawler.interfaces.TermContainer;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.*;

/**
 * Represents a Redis-backed web search index.
 *
 */
public class JedisIndex implements Index {

	private static final String COLON = ":";
	private static final String CRAWLER_PREFIX = "Crawler_";
	private static final String URLSET_PREFIX = "URLSet:";
	private static final String TERM_COUNTER_PREFIX = "TermCounter:";
	private static final String ASTERISK = "*";
	private Jedis jedis;

	/**
	 * Constructor.
	 *
	 * @param jedis
	 */
	public JedisIndex(Jedis jedis) {
		this.jedis = jedis;
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
		return jedis.exists(redisKey);
	}

	/**
	 * Adds a URL to the set associated with `term`.
	 *
	 * @param term
	 * @param url
	 */
	@Override
	public void add(String term, String url) {
		jedis.sadd(urlSetKey(term), url);
	}

	/**
	 * Looks up a search term and returns a set of URLs.
	 *
	 * @param term
	 * @return Set of URLs.
	 */
	@Override
	public Set<String> getURLs(String term) {
		return jedis.smembers(urlSetKey(term));
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
			System.out.println(url + " : " + count);
			if (count != null) {
				map.put(url, count);
			}
		}
		return map;
	}

	/**
	 * Returns the number of times the given term appears at the given URL.
	 *
	 * @param url
	 * @param term
	 * @return	null, if no such entry in the index
	 */
	@Override
	public @Nullable Integer getCount(String url, String term) {
		String redisKey = termCounterKey(url);
		String count = jedis.hget(redisKey, term);
		if (count != null) {
			return new Integer(count);
		} else {
			return null;
		}
	}

	@Override
	public List<String> putTerms(TermContainer tc) {
	    List<String> terms = new ArrayList<>();
		System.out.println("Putting terms in index: ");
		System.out.println(tc);
//		Transaction t = jedis.multi();

		String url = tc.getLabel();
		String hashName = termCounterKey(url);

		// if this page has already been indexed; delete the old hash
		jedis.del(hashName);

		// for each term, add an entry in the termcounter and a new
		// member of the index
		for (String term: tc.keySet()) {
			Integer count = tc.get(term);
			jedis.hset(hashName, term, count.toString());
			jedis.sadd(urlSetKey(term), url);
			terms.add(term);
		}
		return terms;
	}

	/**
	 * Adds vocabulary of page to the index.
	 *
	 * @param tc
	 * @return List of return values from Redis.
	 */
	public List<String> putTermsFaster(TermContainer tc) {
		System.out.println("Putting terms in index: ");
		System.out.println(tc);
		Transaction t = jedis.multi();

		String url = tc.getLabel();
		String hashName = termCounterKey(url);

		// if this page has already been indexed; delete the old hash
		t.del(hashName);

		// for each term, add an entry in the termcounter and a new
		// member of the index
		for (String term: tc.keySet()) {
			Integer count = tc.get(term);
			t.hset(hashName, term, count.toString());
			t.sadd(urlSetKey(term), url);
		}

		t.exec();
		return null;
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
		return jedis.keys(urlSetKey(ASTERISK));
	}

	/**
	 * Returns TermCounter keys for the URLS that have been indexed.
	 *
	 * Should be used for development and testing, not production.
	 *
	 * @return
	 */
	public Set<String> termCounterKeys() {
		return jedis.keys(termCounterKey(ASTERISK));
	}

	/**
	 * Deletes all URLSet objects from the database.
	 *
	 * Should be used for development and testing, not production.
	 *
	 * @return
	 */
	public void deleteURLSets() {
		Set<String> keys = urlSetKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Deletes all URLSet objects from the database.
	 *
	 * Should be used for development and testing, not production.
	 *
	 * @return
	 */
	public void deleteTermCounters() {
		Set<String> keys = termCounterKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Deletes all keys from the database.
	 *
	 * Should be used for development and testing, not production.
	 *
	 * @return
	 */
	public void deleteAllKeys() {
		Set<String> keys = jedis.keys(ASTERISK);
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	@Override
	public void close() {
		jedis.close();
	}
}
