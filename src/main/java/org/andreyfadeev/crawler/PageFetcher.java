/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Allen Downey
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.andreyfadeev.crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.zip.GZIPInputStream;


/**
 * Fetches various elements of web-pages.
 *
 * @author Allen Downey
 * @author Andrey Fadeev
 *
 */
public class PageFetcher {
	private static final String PARAGRAPH = "p";
	private static final String SITEMAP_TAG = "sitemap";
	private static final String URL_TAG = "url";
	private static long lastRequestTime = -1;
	private final long minInterval;

    public PageFetcher() {
        minInterval = 1000;
    }

    public PageFetcher(long minInterval) {
        this.minInterval = minInterval;
    }

    /**
	 * Fetches html-page paragraphs.
	 *
	 * @param url   link to the page
	 * @return      paragraph nodes
	 * @throws IOException
	 */
	public Elements fetchPageParagraphs(String url) throws IOException {
		sleepIfNeeded();

		// download and parse the document
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();

		return doc.select(PARAGRAPH);
	}

	/**
     * Fetches sitemap elements.
     *<p>
     *     Param file can be gzipped.
     *</p>
     *
	 * @param url   link to the sitemap file
	 * @return      sitemap nodes
	 * @throws IOException
	 */
	public Elements fetchSitemapElements(String url) throws IOException {
		Elements content;
		Document doc;
		sleepIfNeeded();

	    if (url.endsWith(".gz")) {
			StringBuilder page = new StringBuilder();
			GZIPInputStream gis = new GZIPInputStream(
										new URL(url).openStream());
			byte[] buffer = new byte[1024];
			int count;

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