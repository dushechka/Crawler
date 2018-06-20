package org.andreyfadeev.crawler;

import org.andreyfadeev.crawler.interfaces.Crawler;
import org.andreyfadeev.crawler.interfaces.Index;
import org.andreyfadeev.crawler.interfaces.TermContainer;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
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

	@Override
	public int queueSize() {
		return queue.size();
	}

	@Override
	public String crawlPage(String link, Index index) throws Exception {
		System.out.println("Crawling " + link);
		URL url = new URL(link);
		String content = ArticleExtractor.INSTANCE.getText(url);
		if (content.isEmpty()) {
			System.out.println("Empty content");
			System.out.println(content);
		} else {
			System.out.println(content);
			TermContainer tc = new TermCounter(link, content);
			index.putTerms(tc);
		}
		return link;
	}

	/**
	 * Crawls pages and saves vocabularies from them
	 * into index.
	 *
	 * @param links links for pages to crawl
	 * @param index index to save vocabularies
	 * @return		set of unavailable pages
	 * @throws IOException	When encounters multiple
	 * 						consistent IO exceptions.
	 */
	@Override
	public Set<String> crawlPages(final Set<String> links, Index index) throws Exception {
		Set<String> unavailable = new HashSet<>();
		int errCounter = 0;
		for (String url : links) {
		    try {
		        crawlPage(url, index);
				errCounter = 0;
			} catch (HttpStatusException | FileNotFoundException e) {
				unavailable.add(url);
				e.printStackTrace();
			} catch (Exception exc) {
		        unavailable.add(url);
		    	errCounter++;
		    	exc.printStackTrace();
		    	if (errCounter > 7) {
		    		throw new Exception(exc);
				}
			}
		}
		return unavailable;
	}

	private String crawlFromQueue() throws IOException {
		if (queue.isEmpty()) {
			return null;
		}
		String url = queue.poll();

		if (index.isIndexed(url)) {
			return null;
		}
		Elements paragraphs = PageFetcher.fetchPageParagraphs(url);
		System.out.println("Crawling " + url);
		TermContainer tc = new TermCounter(url, paragraphs);
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
