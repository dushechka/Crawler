package com.allendowney.thinkdast;

import com.allendowney.thinkdast.interfaces.Crawler;
import com.allendowney.thinkdast.interfaces.Index;
import com.allendowney.thinkdast.interfaces.TermContainer;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;


public class HtmlCrawler implements Crawler {
	private static final String HREF_CSS_QUERY = "a[href]";
	private static final String HREF_ATTR_KEY = "href";

	// the index where the results go
	private Index index;

	// queue of URLs to be indexed
	private Queue<String> queue = new LinkedList<>();

	// fetcher used to get pages from site
	private final static HtmlFetcher hf = new HtmlFetcher();

	/**
	 * Constructor.
	 *
	 * @param index
	 */
	public HtmlCrawler(Index index) {
		this.index = index;
	}

	/**
	 * Returns the number of URLs in the queue.
	 *
	 * @return
	 */
	@Override
	public int queueSize() {
		return queue.size();
	}

	/**
	 * Gets site's pages by given URL.
	 * @param url
	 * @return All site's pages.
	 * @return
	 */
	@Override
	public Set<String> getSitePages(String url) {
		return null;
	}

	/**
	 * Crawls all of the site's links and indexes them.
	 * @param url
     * @return Crawled pages URLs.
	 */
	@Override
	public Set<String> crawl(String url) {
		queue.clear();
		queue.offer(url);
		return null;
	}

	/**
	 * Crawl all of the given links and indexes them.
	 * @param links
	 * @return Crawled pages URLs.
	 */
	@Override
	public Set<String> crawl(Set<String> links) {
		return null;
	}

	/**
	 * Gets a URL from the queue and indexes it.
	 *
	 * @return URL of page indexed.
	 * @throws IOException
	 */
	private String crawlFromQueue() throws IOException {
		if (queue.isEmpty()) {
			return null;
		}
		String url = queue.poll();

		if (index.isIndexed(url)) {
			return null;
		}
		Elements paragraphs = hf.fetchPageParagraphs(url);
		System.out.println("Crawling " + url);
		TermContainer tc = new TermCounter(url, paragraphs);
		index.putTerms(tc);
		queueInternalLinks(paragraphs);
		return url;
	}

	/**
	 * Indexes given url.
	 *
	 * @return	URL of the page indexed.
	 * @throws IOException
	 */
	@Override
	public String crawlPage(String url) throws IOException {
		Elements paragraphs = hf.fetchPageParagraphs(url);
		System.out.println("Crawling " + url);
		TermContainer tc = new TermCounter(url, paragraphs);
		index.putTerms(tc);
		return url;
	}

	/**
	 * Parses paragraphs and adds internal links to the queue.
	 * 
	 * @param paragraphs
	 */
	private void queueInternalLinks(Elements paragraphs) {
		for (Element paragraph: paragraphs) {
			queueInternalLinks(paragraph);
		}
	}

	/**
	 * Parses a paragraph and adds internal links to the queue.
	 * 
	 * @param paragraph
	 */
	private void queueInternalLinks(Element paragraph) {
		Elements elts = paragraph.select(HREF_CSS_QUERY);
		for (Element elt: elts) {
			String relURL = elt.attr(HREF_ATTR_KEY);
			
			if (relURL.startsWith("/wiki/")) {
				String absURL = "https://en.wikipedia.org" + relURL;
				queue.offer(absURL);
			}
		}
	}
}
