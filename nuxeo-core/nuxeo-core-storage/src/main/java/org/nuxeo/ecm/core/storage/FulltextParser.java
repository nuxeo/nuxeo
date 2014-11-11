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

import java.util.List;

/**
 * Parser of strings for fulltext indexing.
 * <p>
 * From the strings extracted from the document, decides how they should be
 * parsed, split and normalized for fulltext indexing by the underlying engine.
 *
 * @since 5.9.5
 */
public interface FulltextParser {

    /**
     * Parses one property value to normalize the fulltext for the database.
     * <p>
     * The passed {@code path} may be {@code null} if the passed string is not
     * coming from a specific path, for instance when it was extracted from
     * binary data.
     *
     * @param s the string to be parsed and normalized
     * @param path the abstracted path for the property (where all complex
     *            indexes have been replaced by {@code *}), or {@code null}
     * @return the normalized words as a single space-separated string
     */
    String parse(String s, String path);

    /**
     * Parses one property value to normalize the fulltext for the database.
     * <p>
     * Like {@link #parse(String, String)} but uses the passed list to
     * accumulate words.
     *
     * @param s the string to be parsed and normalized
     * @param path the abstracted path for the property (where all complex
     *            indexes have been replaced by {@code *}), or {@code null}
     * @param strings the list into which normalized words should be accumulated
     */
    void parse(String s, String path, List<String> strings);

}
