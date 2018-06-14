package com.allendowney.thinkdast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.zip.GZIPInputStream;


public class PageFetcher {
	private static final String PARAGRAPH = "p";
	private static final String SITEMAP_TAG = "sitemap";
	private static final String URL_TAG = "url";
	private static long lastRequestTime = -1;
	private static long minInterval = 1000;

	/**
	 * Fetches and parses a URL string, returning a list of paragraph elements.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static Elements fetchPageParagraphs(String url) throws IOException {
		sleepIfNeeded();

		// download and parse the document
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();

		return doc.select(PARAGRAPH);
	}

	public static Elements fetchSitemapElements(String url) throws IOException {
		Elements content = new Elements();
		Document doc;
		sleepIfNeeded();

	    if (url.endsWith(".gz")) {
			StringBuilder page = new StringBuilder();
			GZIPInputStream gis = new GZIPInputStream(
										new URL(url).openStream());
			byte[] buffer = new byte[1024];
			int count = 0;

			while ((count = gis.read(buffer, 0, 1024)) != -1) {
				page.append(new String(buffer), 0, count);
			}

			doc = Jsoup.parse(page.toString());
			gis.close();
		} else {
			Connection conn = Jsoup.connect(url);
			doc = conn.get();
		}

		content = doc.getElementsByTag(SITEMAP_TAG);
	    content.addAll(doc.getElementsByTag(URL_TAG));

		return content;
	}

	/**
	 * Rate limits by waiting at least the minimum interval between requests.
	 */
	private static void sleepIfNeeded() {
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