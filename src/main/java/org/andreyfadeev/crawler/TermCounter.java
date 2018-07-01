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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.andreyfadeev.crawler.interfaces.TermContainer;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;


/**
 * Encapsulates a map from search term to frequency (count).
 *
 * @author Allen Downey
 * @author Andrey Fadeev
 *
 */
public class TermCounter implements TermContainer {
	private static final String LINE_SEPARATOR = "\n";
	private Map<String, Integer> map = new HashMap<>();
	private String label;

	public TermCounter(String label, Elements paragraphs) {
		this.label = label;
		processElements(paragraphs);
	}

	public TermCounter(String label, String content) {
		this.label = label;
		processText(content);
	}

	@Override
	public String getLabel() {
		return label;
	}

	/**
	 * Returns the total of all counts.
	 *
	 * @return
	 */
	@Override
	public int size() {
		int total = 0;
		for (Integer value: map.values()) {
			total += value;
		}
		return total;
	}

	/**
	 * Takes a collection of Elements and counts their words.
	 *
	 * @param paragraphs
	 */
	private void processElements(Elements paragraphs) {
		for (Node node: paragraphs) {
			processTree(node);
		}
	}

	/**
	 * Finds TextNodes in a DOM tree and counts their words.
	 *
	 * @param root
	 */
	private void processTree(Node root) {
		// NOTE: we could use select to find the TextNodes, but since
		// we already have a tree iterator, let's use it.
		for (Node node: new HtmlNodeIterable(root)) {
			if (node instanceof TextNode) {
				processText(((TextNode) node).text());
			}
		}
	}

	/**
	 * Splits `text` into words and counts them.
	 *
	 * @param text  The text to process.
	 */
	private void processText(String text) {
		// replace punctuation with spaces, convert to lower case, and split on whitespace
		String[] array = text.replaceAll("\\pP", " ").
				              toLowerCase().
				              split("\\pZ+");

		Pattern WORD = Pattern.compile("\\pL{3,}");
		for (int i=0; i<array.length; i++) {
			String term = array[i];
			Matcher m = WORD.matcher(term);
			if (m.find()) {
				incrementTermCount(term);
			}
		}
	}

	/**
	 * Increments the counter associated with `term`.
	 *
	 * @param term
	 */
	@Override
	public void incrementTermCount(String term) {
		put(term, get(term) + 1);
	}

	/**
	 * Adds a term to the map with a given count.
	 *
	 * @param term
	 * @param count
	 */
	@Override
	public void put(String term, int count) {
		map.put(term, count);
	}

	/**
	 * Returns the count associated with this term, or 0 if it is unseen.
	 *
	 * @param term
	 * @return
	 */
	@Override
	public Integer get(String term) {
		Integer count = map.get(term);
		return count == null ? 0 : count;
	}

	/**
	 * Returns the set of terms that have been counted.
	 *
	 * @return
	 */
	@Override
	public Set<String> keySet() {
		return map.keySet();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Termcounter for page: ");
		sb.append(label);
		sb.append(LINE_SEPARATOR);
		for (String key: keySet()) {
			Integer count = get(key);
			sb.append("[");
			sb.append(key);
			sb.append(":");
			sb.append(count);
			sb.append("]");
		}
		sb.append("\nTotal count of words = ");
		sb.append(map.size());
		sb.append("\nTotal of all counts = ");
		sb.append(size());
		sb.append(LINE_SEPARATOR);
		return sb.toString();
	}
}
