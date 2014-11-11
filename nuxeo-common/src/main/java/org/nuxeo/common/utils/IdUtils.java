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
 *     Nuxeo - initial API and implementation
 *
 * $Id: IdUtils.java 19046 2007-05-21 13:03:50Z sfermigier $
 */

package org.nuxeo.common.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Utils for identifier generation.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public final class IdUtils {

    private static final String WORD_SPLITTING_REGEXP = "[^a-zA-Z0-9]+";

    public static final String UUID_TYPE_4_REGEXP = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}";

    // TODO AT: dummy random, does not ensure uniqueness
    private static final Random RANDOM = new Random(new Date().getTime());

    // This is an utility class.
    private IdUtils() {
    }

    /**
     * Generates an unique string identifier.
     */
    public static String generateStringId() {
        return String.valueOf(generateLongId());
    }

    /**
     * Generates an unique long identifier.
     */
    public static long generateLongId() {
        long r = RANDOM.nextLong();
        if (r < 0) {
            r = -r;
        }
        return r;
    }

    /**
     * Generates an id from a non-null String.
     * <p>
     * Replaces accented characters from a string by their ascii equivalent,
     * removes non alphanumerical characters and replaces spaces by the given
     * wordSeparator character.
     *
     * @param s the original String
     * @param wordSeparator the word separator to use (usually '-')
     * @param lower if lower is true, remove upper case
     * @param maxChars maximum longer of identifier characters
     * @return the identifier String
     */
    public static String generateId(String s, String wordSeparator,
            boolean lower, int maxChars) {
        s = StringUtils.toAscii(s);
        s = s.trim();
        if (lower) {
            s = s.toLowerCase();
        }
        String[] words = s.split(WORD_SPLITTING_REGEXP);
        // remove blank chars from words, did not get why they're not filtered
        List<String> wordsList = new ArrayList<String>();
        for (String word : words) {
            if (word != null && word.length() > 0) {
                wordsList.add(word);
            }
        }
        if (wordsList.isEmpty()) {
            return generateStringId();
        }
        StringBuilder sb = new StringBuilder();
        String id;
        if (maxChars > 0) {
            // be sure at least one word is used
            sb.append(wordsList.get(0));
            for (int i = 1; i < wordsList.size(); i++) {
                String newWord = wordsList.get(i);
                if (sb.length() + newWord.length() > maxChars) {
                    break;
                } else {
                    sb.append(wordSeparator).append(newWord);
                }
            }
            id = sb.toString();
            id = id.substring(0, Math.min(id.length(), maxChars));
        } else {
            id = StringUtils.join(wordsList.toArray(), wordSeparator);
        }

        return id;
    }

    /**
     * Generates an id from a non-null String.
     * <p>
     * Uses default values for wordSeparator: '-', lower: true, maxChars: 24.
     *
     * @deprecated use {@link #generatePathSegment} instead, or
     *             {@link #generateId(String, String, boolean, int)} depending
     *             on the use cases
     */
    @Deprecated
    public static String generateId(String s) {
        return generateId(s, "-", true, 24);
    }

    public static final Pattern STUPID_REGEXP = Pattern.compile("^[- .,;?!:/\\\\'\"]*$");

    /**
     * Generates a Nuxeo path segment from a non-null String.
     * <p>
     * Basically all characters are kept, except for slashes and
     * initial/trailing spaces.
     *
     * @deprecated use {@link PathSegmentService} instead
     */
    @Deprecated
    public static String generatePathSegment(String s) {
        s = s.trim();
        if (STUPID_REGEXP.matcher(s).matches()) {
            return generateStringId();
        }
        return s.replace("/", "-");
    }

    /**
     * Check if a given string has the pattern for UUID type 4
     * 
     * @since 5.7
     */
    public static boolean isValidUUID(String uuid) {
        if (Pattern.compile(UUID_TYPE_4_REGEXP).matcher(uuid).matches()) {
            return true;
        }
        return false;
    }

}
