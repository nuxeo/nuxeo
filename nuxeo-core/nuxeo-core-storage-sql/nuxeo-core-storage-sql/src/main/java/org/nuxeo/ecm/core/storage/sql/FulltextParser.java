/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

import org.apache.commons.lang.StringEscapeUtils;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.runtime.api.Framework;

/**
 * Parser of strings for fulltext indexing.
 * <p>
 * Takes strings extracted from the document, and decides which preprocessed
 * strings to pass to the underlying database for native indexing.
 * 
 * @since 5.6, 5.5.0-HF03
 */
public class FulltextParser {

    protected static final String WORD_SPLIT_PROP = "org.nuxeo.vcs.fulltext.wordsplit";

    protected static final String WORD_SPLIT_DEF = "[\\s\\p{Punct}]+";

    protected static final Pattern WORD_SPLIT_PATTERN = Pattern.compile(Framework.getProperty(
            WORD_SPLIT_PROP, WORD_SPLIT_DEF));

    protected Node document;

    protected SessionImpl session;

    protected String documentType;

    protected String[] mixinTypes;

    protected String indexName;

    protected Set<String> paths;

    protected ArrayList<String> strings;

    public ArrayList<String> getStrings() {
        return strings;
    }

    public void setStrings(ArrayList<String> strings) {
        this.strings = strings;
    }

    /**
     * Prepares parsing for one document.
     */
    protected void setDocument(Node document, SessionImpl session) {
        this.document = document;
        this.session = session;
        if (document != null) { // null in tests
            documentType = document.getPrimaryType();
            mixinTypes = document.getMixinTypes();
        }
    }

    /**
     * Parses the document for one index.
     */
    protected String findFulltext(String indexName, Set<String> paths)
            throws StorageException {
        if (paths == null) {
            return "";
        }
        this.indexName = indexName;
        this.paths = paths;
        strings = new ArrayList<String>();

        for (String path : paths) {
            ModelProperty pi = session.getModel().getPathPropertyInfo(
                    documentType, mixinTypes, path);
            if (pi == null) {
                continue; // doc type doesn't have this property
            }
            if (pi.propertyType != PropertyType.STRING
                    && pi.propertyType != PropertyType.ARRAY_STRING) {
                continue;
            }

            List<Node> nodes = new ArrayList<Node>(
                    Collections.singleton(document));

            String[] names = path.split("/");
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                if (i < names.length - 1) {
                    // traverse
                    List<Node> newNodes;
                    if ("*".equals(names[i + 1])) {
                        // traverse complex list
                        i++;
                        newNodes = new ArrayList<Node>();
                        for (Node node : nodes) {
                            newNodes.addAll(session.getChildren(node, name,
                                    true));
                        }
                    } else {
                        // traverse child
                        newNodes = new ArrayList<Node>(nodes.size());
                        for (Node node : nodes) {
                            node = session.getChildNode(node, name, true);
                            if (node != null) {
                                newNodes.add(node);
                            }
                        }
                    }
                    nodes = newNodes;
                } else {
                    // last path component: get value
                    for (Node node : nodes) {
                        if (pi.propertyType == PropertyType.STRING) {
                            String v = node.getSimpleProperty(name).getString();
                            if (v != null) {
                                parse(v, path);
                            }
                        } else { /* ARRAY_STRING */
                            for (Serializable v : node.getCollectionProperty(
                                    name).getValue()) {
                                if (v != null) {
                                    parse((String) v, path);
                                }
                            }
                        }
                    }
                }
            }
        }

        return StringUtils.join(strings, " ");
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
