/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Default fulltext parser, based on word and punctuation split, and lowercase
 * normalization.
 * <p>
 * The regexp used can be configured using the system property
 * {@value #WORD_SPLIT_PROP}. The default is {@value #WORD_SPLIT_DEF}.
 *
 * @since 5.9.5
 */
public class DefaultFulltextParser implements FulltextParser {

    public static final String WORD_SPLIT_PROP = "org.nuxeo.fulltext.wordsplit";

    public static final String WORD_SPLIT_DEF = "[\\s\\p{Punct}]+";

    protected static final Pattern WORD_SPLIT_PATTERN = Pattern.compile(Framework.getProperty(
            WORD_SPLIT_PROP, WORD_SPLIT_DEF));

    @Override
    public String parse(String s, String path) {
        List<String> strings = new ArrayList<>();
        parse(s, path, strings);
        return StringUtils.join(strings, ' ');
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default implementation normalizes text to lowercase and removes
     * punctuation.
     * <p>
     * This can be subclassed.
     */
    @Override
    public void parse(String s, String path, List<String> strings) {
        s = preprocessField(s, path);
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
    protected String preprocessField(String s, String path) {
        if (s == null) {
            return null;
        }
        if (s.contains("<")) {
            s = removeHtml(s);
        }
        return StringEscapeUtils.unescapeHtml(s);
    }

    protected String removeHtml(String s) {
        Source source = new Source(s);
        Renderer renderer = source.getRenderer();
        renderer.setIncludeHyperlinkURLs(false);
        renderer.setDecorateFontStyles(false);
        return renderer.toString();
    }

}
