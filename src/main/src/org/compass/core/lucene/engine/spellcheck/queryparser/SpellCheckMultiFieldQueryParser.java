/*
 * Copyright 2004-2009 the original author or authors.
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

package org.compass.core.lucene.engine.spellcheck.queryparser;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.CompassMultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.spell.CompassSpellChecker;
import org.compass.core.engine.SearchEngineFactory;
import org.compass.core.lucene.engine.spellcheck.InternalLuceneSearchEngineSpellCheckManager;
import org.compass.core.mapping.CompassMapping;

/**
 * @author kimchy
 */
public class SpellCheckMultiFieldQueryParser extends CompassMultiFieldQueryParser {

    private CompassSpellChecker spellChecker;

    public SpellCheckMultiFieldQueryParser(String[] fields, Map<String, Float> boosts, Analyzer analyzer, CompassMapping mapping, SearchEngineFactory searchEngineFactory, boolean forceAnalyzer) {
        super(fields, boosts, analyzer, mapping, searchEngineFactory, forceAnalyzer);
        InternalLuceneSearchEngineSpellCheckManager spellCheckManager = (InternalLuceneSearchEngineSpellCheckManager) searchEngineFactory.getSpellCheckManager();
        this.spellChecker = spellCheckManager.createSpellChecker(null, null);
    }

    protected Term getTerm(String field, String text) throws ParseException {
        try {
            if (spellChecker.exist(text)) {
                return super.getTerm(field, text);
            }
            String[] similarWords = spellChecker.suggestSimilar(text, 1);
            if (similarWords.length == 0) {
                return super.getTerm(field, text);
            }
            suggestedQuery = true;
            return super.getTerm(field, similarWords[0]);
        } catch (IOException e) {
            throw new ParseException("Failed to spell check suggest " + e.getMessage());
        }
    }

    public void close() {
        spellChecker.close();
        super.close();
    }
}
