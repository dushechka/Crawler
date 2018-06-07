package com.allendowney.thinkdast;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.allendowney.thinkdast.interfaces.TermContainer;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;


/**
 * Encapsulates a map from search term to frequency (count).
 *
 * @author downey
 *
 */
public class TermCounter implements TermContainer {

	public static final String LINE_SEPARATOR = "\n";
	private Map<String, Integer> map;
	private String label;

	public TermCounter(String label, Elements paragraphs) {
		this.label = label;
		this.map = new HashMap<>();
		processElements(paragraphs);
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
				              split("\\s+");
		
		for (int i=0; i<array.length; i++) {
			String term = array[i];
			incrementTermCount(term);
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
//		for (String key: keySet()) {
//			Integer count = get(key);
//			sb.append(key);
//			sb.append(", ");
//			sb.append(count);
//			sb.append(LINE_SEPARATOR);
//		}
		sb.append("Total of all counts = ");
		sb.append(size());
		sb.append(LINE_SEPARATOR);
		return sb.toString();
	}
}
