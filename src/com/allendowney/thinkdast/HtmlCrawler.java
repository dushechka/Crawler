package com.allendowney.thinkdast;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import com.allendowney.thinkdast.interfaces.Index;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class HtmlCrawler {
	public static final String HREF_CSS_QUERY = "a[href]";
	public static final String HREF_ATTR_KEY = "href";
	// keeps track of where we started
	@SuppressWarnings("unused")
	private final String source;

	// the index where the results go
	private Index index;

	// queue of URLs to be indexed
	private Queue<String> queue = new LinkedList<>();

	// fetcher used to get pages from Wikipedia
	private final static HtmlFetcher hf = new HtmlFetcher();

	/**
	 * Constructor.
	 *
	 * @param source
	 * @param index
	 */
	public HtmlCrawler(String source, Index index) {
		this.source = source;
		this.index = index;
		queue.offer(source);
	}

	/**
	 * Returns the number of URLs in the queue.
	 *
	 * @return
	 */
	public int queueSize() {
		return queue.size();
	}

	/**
	 * Gets a URL from the queue and indexes it.
	 * @param testing
	 *
	 * @return URL of page indexed.
	 * @throws IOException
	 */
	public String crawl(boolean testing) throws IOException {
		if (queue.isEmpty()) {
			return null;
		}
		String url = queue.poll();
		System.out.println("Crawling " + url);

		if (index.isIndexed(url)) {
			return null;
		}
		
		Elements paragraphs = hf.fetchPageParagraphs(url);
		TermCounter tc = new TermCounter(url, paragraphs);
		index.putTerms(tc);
		queueInternalLinks(paragraphs);		
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
