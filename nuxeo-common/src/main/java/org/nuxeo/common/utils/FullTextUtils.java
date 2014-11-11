/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.common.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Functions related to simple fulltext parsing. They don't try to be exhaustive
 * but they work for simple cases.
 */
public class FullTextUtils {

    public static final Pattern wordPattern = Pattern.compile("[\\s\\p{Punct}]+");

    public static final int MIN_SIZE = 3;

    public static final String STOP_WORDS = "a an are and as at be by for from how "
            + "i in is it of on or that the this to was what when where who will with "
            + "car donc est il ils je la le les mais ni nous or ou pour tu un une vous "
            + "www com net org";

    public static final Set<String> stopWords = new HashSet<String>(
            Arrays.asList(StringUtils.split(STOP_WORDS, ' ', false)));

    public static final String UNACCENTED = "aaaaaaaceeeeiiii\u00f0nooooo\u00f7ouuuuy\u00fey";

    private FullTextUtils() {
        // utility class
    }

    /**
     * Extracts the words from a string for simple fulltext indexing.
     * <p>
     * Initial order is kept, but duplicate words are removed.
     * <p>
     * It omits short or stop words, removes accents and does pseudo-stemming.
     *
     * @param string the string
     * @param removeDiacritics if the diacritics must be removed
     * @return an ordered set of resulting words
     */
    public static Set<String> parseFullText(String string,
            boolean removeDiacritics) {
        if (string == null) {
            return Collections.emptySet();
        }
        Set<String> set = new LinkedHashSet<String>();
        for (String word : wordPattern.split(string)) {
            String w = parseWord(word, removeDiacritics);
            if (w != null) {
                set.add(w);
            }
        }
        return set;
    }

    /**
     * Parses a word and returns a simplified lowercase form.
     *
     * @param string the word
     * @param removeDiacritics if the diacritics must be removed
     * @return the simplified word, or {@code null} if it was removed as a stop
     *         word or a short word
     */
    public static String parseWord(String string, boolean removeDiacritics) {
        int len = string.length();
        if (len < MIN_SIZE) {
            return null;
        }
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = Character.toLowerCase(string.charAt(i));
            if (removeDiacritics) {
                if (c == '\u00e6') {
                    buf.append("ae");
                } else if (c >= '\u00e0' && c <= '\u00ff') {
                    buf.append(UNACCENTED.charAt((c) - 0xe0));
                } else if (c == '\u0153') {
                    buf.append("oe");
                } else {
                    buf.append(c);
                }
            } else {
                buf.append(c);
            }
        }
        // simple heuristic to remove plurals
        int l = buf.length();
        if (l > 3 && buf.charAt(l - 1) == 's') {
            buf.setLength(l - 1);
        }
        String word = buf.toString();
        if (stopWords.contains(word)) {
            return null;
        }
        return word;
    }
}
