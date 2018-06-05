package com.allendowney.thinkdast;

import org.jsoup.nodes.Node;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SitemapLinksIterable implements Iterable<Node> {

    private URL root;

    public SitemapLinksIterable(URL root) {
        this.root = root;
    }

    @Override
    public Iterator<Node> iterator() {
        return new SitemapLinksIterator(root);
    }

    private class SitemapLinksIterator implements Iterator<Node> {
        private Deque<Node> stack;

        public SitemapLinksIterator(URL root) {
            stack = new ArrayDeque<>();
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public Node next() {
            if (stack.isEmpty()) {
                throw new NoSuchElementException();
            }

            return null;
        }
    }
}
