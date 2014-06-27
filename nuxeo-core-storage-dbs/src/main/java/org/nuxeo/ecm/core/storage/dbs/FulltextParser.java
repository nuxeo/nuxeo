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
package org.nuxeo.ecm.core.storage.dbs;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_MIXIN_TYPES;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PREFIX;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.runtime.api.Framework;

/**
 * Parser of strings for fulltext indexing.
 * <p>
 * Takes strings extracted from the document, and decides which preprocessed
 * strings to pass to the underlying database for native indexing.
 *
 * @since 5.9.5
 */
public class FulltextParser {

    protected static final String WORD_SPLIT_PROP = "org.nuxeo.vcs.fulltext.wordsplit";

    protected static final String WORD_SPLIT_DEF = "[\\s\\p{Punct}]+";

    protected static final Pattern WORD_SPLIT_PATTERN = Pattern.compile(Framework.getProperty(
            WORD_SPLIT_PROP, WORD_SPLIT_DEF));

    protected DBSDocumentState document;

    protected DBSSession session;

    protected String documentType;

    protected Object[] mixinTypes;

    protected String indexName;

    protected Set<String> paths;

    protected ArrayList<String> strings;

    public FulltextParser() {
    }

    public ArrayList<String> getStrings() {
        return strings;
    }

    public void setStrings(ArrayList<String> strings) {
        this.strings = strings;
    }

    /**
     * Prepares parsing for one document.
     */
    protected void setDocument(DBSDocumentState document, DBSSession session) {
        this.document = document;
        this.session = session;
        if (document != null) { // null in tests
            documentType = document.getPrimaryType();
            mixinTypes = (Object[]) document.get(KEY_MIXIN_TYPES);
        }
    }

    /**
     * Parses the document for one index.
     */
    public String findFulltext(String indexName) {
        this.indexName = indexName;
        // TODO paths
        strings = new ArrayList<String>();

        State state = document.getState();
        findFulltext(indexName, state);

        return StringUtils.join(strings, ' ');
    }

    protected void findFulltext(String indexName, State state) {
        for (Entry<String, Serializable> en : state.entrySet()) {
            String key = en.getKey();
            if (key.startsWith(KEY_PREFIX)) {
                switch (key) {
                // allow indexing of this:
                case DBSDocument.KEY_NAME:
                    break;
                default:
                    continue;
                }
            }
            Serializable value = en.getValue();
            if (value instanceof State) {
                State s = (State) value;
                findFulltext(indexName, s);
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<State> v = (List<State>) value;
                for (State s : v) {
                    findFulltext(indexName, s);
                }
            } else if (value instanceof Object[]) {
                Object[] ar = (Object[]) value;
                for (Object v : ar) {
                    if (v instanceof String) {
                        parse((String) v, null);
                    } else {
                        // arrays are homogeneous, no need to continue
                        break;
                    }
                }
            } else {
                if (value instanceof String) {
                    parse((String) value, null);
                }
            }
        }

    }

    /**
     * Parses one property value to normalize the fulltext for the database.
     * <p>
     * The default implementation normalizes text to lowercase and removes
     * punctuation.
     * <p>
     * This can be subclassed. Implementations should append to the
     * {@link #strings} list.
     *
     * @param s the string to be parsed and normalized
     * @param path the abstracted path for the property, where all complex
     *            indexes have been replaced by {@code *}
     */
    public void parse(String s, String path) {
        s = preprocessField(s, path);
        for (String word : WORD_SPLIT_PATTERN.split(s)) {
            if (!word.isEmpty()) {
                strings.add(word.toLowerCase());
            }
        }
    }

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
