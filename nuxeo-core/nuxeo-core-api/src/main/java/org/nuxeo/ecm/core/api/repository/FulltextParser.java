/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api.repository;

import org.nuxeo.ecm.core.api.DocumentLocation;

import java.util.List;

/**
 * Parser of strings for fulltext indexing.
 * <p>
 * From the strings extracted from the document, decides how they should be parsed, split and normalized for fulltext
 * indexing by the underlying engine.
 *
 * @since 5.9.5
 */
public interface FulltextParser {

    /**
     * Parses one property value to normalize the fulltext for the database.
     * <p>
     * The passed {@code path} may be {@code null} if the passed string is not coming from a specific path, for instance
     * when it was extracted from binary data.
     *
     * @param s the string to be parsed and normalized
     * @param path the abstracted path for the property (where all complex indexes have been replaced by {@code *}), or
     *            {@code null}
     * @return the normalized words as a single space-separated string
     */
    String parse(String s, String path);

    /**
     * Parses one property value to normalize the fulltext for the database.
     * <p>
     * Like {@link #parse(String, String)} but uses the passed list to accumulate words.
     *
     * @param s the string to be parsed and normalized
     * @param path the abstracted path for the property (where all complex indexes have been replaced by {@code *}), or
     *            {@code null}
     * @param strings the list into which normalized words should be accumulated
     */
    void parse(String s, String path, List<String> strings);

    /**
     * Parses one property value to normalize the fulltext for the database.
     * <p>
     * The passed {@code path} may be {@code null} if the passed string is not coming from a specific path, for instance
     * when it was extracted from binary data.
     *
     * @param s the string to be parsed and normalized
     * @param path the abstracted path for the property (where all complex indexes have been replaced by {@code *}), or
     *            {@code null}
     * @param mimeType the {@code mimeType} of the string to be parsed and normalized. This may be {@code null}
     * @param documentLocation the {@code documentLocation} of the Document from which the property value string
     *            was extracted. This may be {@code null}
     * @return the normalized words as a single space-separated string
     * @since 8.4
     */
    String parse(String s, String path, String mimeType, DocumentLocation documentLocation);

    /**
     * Parses one property value to normalize the fulltext for the database.
     * <p>
     * Like {@link #parse(String, String)} but uses the passed list to accumulate words.
     *
     * @param s the string to be parsed and normalized
     * @param path the abstracted path for the property (where all complex indexes have been replaced by {@code *}), or
     *            {@code null}
     * @param mimeType the {@code mimeType} of the string to be parsed and normalized. This may be {@code null}
     * @param documentLocation the {@code documentLocation} of the Document from which the property value string
     *            was extracted. This may be {@code null}
     * @param strings the list into which normalized words should be accumulated
     * @since 8.4
     */
    void parse(String s, String path, String mimeType, DocumentLocation documentLocation, List<String> strings);

}
