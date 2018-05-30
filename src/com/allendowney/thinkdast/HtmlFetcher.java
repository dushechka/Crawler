package com.allendowney.thinkdast;

import java.io.IOException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class HtmlFetcher {
	private static final String MW_CONTENT_TEXT = "mw-content-text";
	public static final String PARAGRAPH = "p";
	private long lastRequestTime = -1;
	private long minInterval = 1000;

	/**
	 * Fetches and parses a URL string, returning a list of paragraph elements.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Elements fetchPageParagraphs(String url) throws IOException {
		sleepIfNeeded();

		// download and parse the document
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();

		// select the content text and pull out the paragraphs.
		Element content = doc.getElementById(MW_CONTENT_TEXT);

		Elements paras = content.select(PARAGRAPH);
		return paras;
	}

	/**
	 * Rate limits by waiting at least the minimum interval between requests.
	 */
	private void sleepIfNeeded() {
		if (lastRequestTime != -1) {
			long currentTime = System.currentTimeMillis();
			long nextRequestTime = lastRequestTime + minInterval;
			if (currentTime < nextRequestTime) {
				try {
					Thread.sleep(nextRequestTime - currentTime);
				} catch (InterruptedException e) {
					System.err.println("Warning: sleep interrupted in fetchPageParagraphs.");
				}
			}
		}
		lastRequestTime = System.currentTimeMillis();
	}
}