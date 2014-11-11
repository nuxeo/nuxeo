/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileNamePattern {

    private boolean wstart = false;
    private boolean wend = false;
    private char[][] parts;

    public FileNamePattern(String pattern) {
        initialize(pattern);
    }

    private void initialize(String pattern) {
        if (pattern == null || pattern.length() == 0) {
            return;
        }
        List<char[]> result = new ArrayList<char[]>();
        StringBuffer buf = new StringBuffer();
        char[] chars = pattern.toCharArray();
        char c = chars[0];
        int i = 0;
        if (c == '*') {
            wstart = true;
            i = 1;
        }
        int len;
        for (; i < chars.length; i++) {
            c = chars[i];
            switch (c) {
            case '*':
                len = buf.length();
                if (len > 0) {
                    result.add(toCharArray(buf, len));
                    buf.setLength(0);
                }
                break;
            default:
                buf.append(c);
                break;
            }
        }
        if (c == '*') {
            wend = true;
        } else {
            len = buf.length();
            if (len > 0) {
                result.add(toCharArray(buf, len));
            }
        }
        parts = result.toArray(new char[result.size()][]);
    }

    private static char[] toCharArray(StringBuffer buf, int len) {
        char[] part = new char[len];
        buf.getChars(0, len, part, 0);
        return part;
    }

    public boolean match(String text) {
        if (parts == null || parts.length == 0) {
            if (text.length() == 0) { // handle "" empty patterns
                return true;
            }
            return wstart || wend;
        }

        return match(text.toCharArray(), 0, 0, !wstart);
    }


    private boolean match(char[] text, int offset, int part, boolean exactMatch) {
        if (part >= parts.length) {
            int len = text.length;
            if (offset > len) {
                return false;
            } else if (offset == len) {
                return true;
            } else {
                return wend;
            }
        }
        char[] pattern = parts[part];
        // no match founds - try next segment matches
        if (exactMatch) {
            int k = offset + pattern.length;
            if (k > text.length) {
                return false;
            }
            if (containsAt(text, offset, pattern)) {
                return match(text, k, part + 1, false);
            }
            return false;
        }
        int k = offset;
        while (true) {
            k = indexOf(text, pattern, k);
            if (k == -1) {
                return false;
            }
            if (match(text, k + pattern.length, part + 1, false)) {
                return true; // matched
            }
            // not matched -> continue using next matching segments
            k++;
        }
    }

    /**
     * Variant of indexOf with ? wildcard.
     */
    public static int indexOf(char[] chars, char[] pattern, int offset) {
        // do not iterate if not needed
        if (pattern.length > chars.length - offset) {
            // not enough chars in the chars array starting at offset to match the given pattern
            return -1;
        }
        if (pattern.length == 0) {
            return 0;
        }
        // get the number of possible matching offsets (past over this offset
        // matching is no more possible)
        int len = chars.length - pattern.length + 1;
        START: for (int i = offset; i < len; i++) {
            char c = pattern[0];
            if (chars[i] == c || c == '?') {
                // find first char -> now look to the rest of the pattern
                int k = 1;
                for (; k < pattern.length; k++) {
                    c = pattern[k];
                    if (chars[k + i] != c && c != '?') {
                        continue START;
                    }
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * Tests whether the given array match the pattern at the given position.
     * Matching allows ? wildcards.
     */
    public static boolean containsAt(char[] array, int offset, char[] pattern) {
        if (offset + pattern.length > array.length) {
            return false;
        }
        if (pattern.length == 0) {
            return true;
        }
        if (array[offset] != pattern[0] && pattern[0] != '?') {
            return false;
        }
        for (int i = 1; i < pattern.length; i++) {
            int k = offset + i;
            if (array[k] != pattern[i] && pattern[i] != '?') {
                return false;
            }
        }
        return true;
    }

}
