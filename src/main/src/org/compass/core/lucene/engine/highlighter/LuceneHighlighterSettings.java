/*
 * Copyright 2004-2006 the original author or authors.
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

package org.compass.core.lucene.engine.highlighter;

import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.compass.core.CompassHighlighter;
import org.compass.core.config.CompassConfigurable;
import org.compass.core.engine.SearchEngineException;

/**
 * 
 * @author kimchy
 *
 */
public interface LuceneHighlighterSettings extends CompassConfigurable {

    public Encoder getEncoder() throws SearchEngineException;

    public Formatter getFormatter() throws SearchEngineException;

    public Fragmenter getFragmenter() throws SearchEngineException;

    boolean isRewriteQuery();
    
    boolean isComputeIdf();
    
    int getMaxNumFragments();
    
    String getSeparator();
    
    int getMaxBytesToAnalyze();
    
    CompassHighlighter.TextTokenizer getTextTokenizer();
}
