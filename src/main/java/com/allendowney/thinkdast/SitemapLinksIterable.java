package com.allendowney.thinkdast;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class SitemapLinksIterable implements Iterable<Element> {
    private static final String LOC_TAG = "loc";
    private String root;
    private Deque<Element> stack;

    public SitemapLinksIterable(String root) {
        this.root = root;
        stack = new ArrayDeque<>();
    }

    private void pushNodesToStack(Elements nodes) throws IOException {
        Collections.reverse(nodes);
        for (Element node : nodes) {
            stack.push(node);
        }
    }

    @Override
    public Iterator<Element> iterator() {
        try {
            Elements nodes = PageFetcher.fetchSitemapElements(root);
            pushNodesToStack(nodes);
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        return new SitemapLinksIterator();
    }

    public class SitemapLinksIterator implements Iterator<Element> {

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public Element next() {
            if (stack.isEmpty()) {
                throw new NoSuchElementException();
            }

            Element node = stack.pop();
            if (node.tagName().equals("url")) {
                return node;
            } else {
                try {
                    pushNodesToStack(
                            PageFetcher.fetchSitemapElements(
                                    node.getElementsByTag(LOC_TAG).text()));
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
                return next();
            }
        }
    }
}
