/*
 * Copyright (c) 2018 Andrey Fadeev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andreyfadeev.crawler;

import org.andreyfadeev.crawler.interfaces.TermContainer;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import org.jetbrains.annotations.Nullable;
import org.jsoup.HttpStatusException;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;


/**
 * Parses web-pages and returns counts of words from them.
 *
 * @author Andrey Fadeev
 */
public class Parser {

	public @Nullable TermContainer parsePage(String link) throws Exception {
		System.out.println("Crawling " + link);
		URL url = new URL(link);
		String content = ArticleExtractor.INSTANCE.getText(url);
		if (content.isEmpty()) {
			System.out.println("Empty content");
			System.out.println(content);
			return null;
		} else {
			System.out.println(content);
			return new TermCounter(link, content);
		}
	}

	public Set<TermContainer> parsePages(final Set<String> links) throws Exception {
		Set<TermContainer> parsed = new HashSet<>();
		int errCounter = 0;
		for (String url : links) {
		    try {
		    	TermContainer tc = parsePage(url);
		    	if (tc != null) {
		    		parsed.add(parsePage(url));
				}
				errCounter = 0;
			} catch (HttpStatusException | FileNotFoundException e) {
				e.printStackTrace();
			} catch (Exception exc) {
		    	errCounter++;
		    	exc.printStackTrace();
		    	if (errCounter > 7) {
		    		throw new Exception(exc);
				}
			}
		}
		return parsed;
	}
}
