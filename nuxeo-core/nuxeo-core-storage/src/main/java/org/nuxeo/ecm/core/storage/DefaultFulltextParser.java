/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.repository.FulltextParser;
import org.nuxeo.runtime.api.Framework;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

/**
 * Default fulltext parser, based on word and punctuation split, and lowercase normalization.
 * <p>
 * The regexp used can be configured using the system property {@value #WORD_SPLIT_PROP}. The default is
 * {@value #WORD_SPLIT_DEF}.
 *
 * @since 5.9.5
 */
public class DefaultFulltextParser implements FulltextParser {

    public static final String WORD_SPLIT_PROP = "org.nuxeo.fulltext.wordsplit";

    public static final String WORD_SPLIT_DEF = "[\\s\\p{Punct}]+";

    protected static final Pattern WORD_SPLIT_PATTERN = Pattern.compile(
            Framework.getProperty(WORD_SPLIT_PROP, WORD_SPLIT_DEF));

    protected static final int HTML_MAGIC_OFFSET = 8192;

    protected static final String TEXT_HTML = "text/html";

    @Override
    public String parse(String s, String path) {
        return parse(s, path, null, null);
    }

    @Override
    public void parse(String s, String path, List<String> strings) {
        parse(s, path, null, null, strings);
    }

    @Override
    public String parse(String s, String path, String mimeType, DocumentLocation documentLocation) {
        List<String> strings = new ArrayList<>();
        parse(s, path, mimeType, documentLocation, strings);
        return StringUtils.join(strings, ' ');
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation normalizes text to lowercase and removes punctuation. The documentLocation parameter
     * is currently unused but has some use cases for potential subclasses.
     * <p>
     * This can be subclassed.
     */
    @Override
    public void parse(String s, String path, String mimeType, DocumentLocation documentLocation, List<String> strings) {
        s = preprocessField(s, path, mimeType);
        for (String word : WORD_SPLIT_PATTERN.split(s)) {
            if (!word.isEmpty()) {
                strings.add(word.toLowerCase());
            }
        }
    }

    /**
     * Preprocesses one field at the given path.
     * <p>
     * The path is unused for now.
     */
    protected String preprocessField(String s, String path, String mimeType) {
        if (s == null) {
            return null;
        }
        if (StringUtils.isEmpty(mimeType)) {
            // Use weak HTML detection here since nuxeo-core-mimetype 'magic.xml' has text/html detection commented
            String htmlMagicExtraction = s.substring(0, Math.min(s.length(), HTML_MAGIC_OFFSET));
            String htmlMagicExtractionLC = htmlMagicExtraction.toLowerCase();
            if (htmlMagicExtractionLC.startsWith("<!doctype html") || htmlMagicExtractionLC.contains("<html")) {
                mimeType = TEXT_HTML;
            }
        }
        if (TEXT_HTML.equals(mimeType)) {
            s = removeHtml(s);
        }
        return StringEscapeUtils.unescapeHtml4(s);
    }

    protected String removeHtml(String s) {
        Source source = new Source(s);
        Renderer renderer = source.getRenderer();
        renderer.setIncludeHyperlinkURLs(false);
        renderer.setDecorateFontStyles(false);
        return renderer.toString();
    }

}
