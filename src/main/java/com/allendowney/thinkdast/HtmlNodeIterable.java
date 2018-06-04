/**
 * 
 */
package com.allendowney.thinkdast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.jsoup.nodes.Node;


/**
 * Performs a depth-first traversal of a jsoup Node.
 *
 * @author downey
 *
 */
public class HtmlNodeIterable implements Iterable<Node> {

	private Node root;

	/**
	 * Creates an iterable starting with the given Node.
	 *
	 * @param root
	 */
	public HtmlNodeIterable(Node root) {
	    this.root = root;
	}

	@Override
	public Iterator<Node> iterator() {
		return new HtmlNodeIterator(root);
	}

	/**
	 * Inner class that implements the Iterator.
	 *
	 * @author downey
	 *
	 */
	private class HtmlNodeIterator implements Iterator<Node> {

		// this stack keeps track of the Nodes waiting to be visited
		Deque<Node> stack;

		/**
		 * Initializes the Iterator with the root Node on the stack.
		 *
		 * @param node
		 */
		public HtmlNodeIterator(Node node) {
			stack = new ArrayDeque<>();
		    stack.push(node);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public Node next() {
			// if the stack is empty, we're done
			if (stack.isEmpty()) {
				throw new NoSuchElementException();
			}

			// otherwise pop the next Node off the stack
			Node node = stack.pop();

			// push the children onto the stack in reverse order
			List<Node> nodes = new ArrayList<>(node.childNodes());
			Collections.reverse(nodes);
			for (Node child: nodes) {
				stack.push(child);
			}
			return node;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
