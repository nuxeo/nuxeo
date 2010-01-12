/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.chemistry.shell.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StringUtils {

    private StringUtils() {
    }

    public static String[] split(String str, char delimiter, boolean trim) {
        int s = 0;
        int e = str.indexOf(delimiter, s);
        if (e == -1) {
            if (trim) {
                str = str.trim();
            }
            return new String[] {str};
        }
        List<String> ar = new ArrayList<String>();
        do {
            String segment = str.substring(s, e);
            if (trim) {
                segment = segment.trim();
            }
            ar.add(segment);
            s = e + 1;
            e = str.indexOf(delimiter, s);
        } while (e != -1);

        int len = str.length();
        if (s < len) {
            String segment = str.substring(s);
            if (trim) {
                segment = segment.trim();
            }
            ar.add(segment);
        } else {
            ar.add("");
        }

        return ar.toArray(new String[ar.size()]);
    }

    public static String[] tokenize(String text) {
        boolean esc = false;
        boolean inString = false;
        ArrayList<String> tokens = new ArrayList<String>();
        StringBuilder buf = new StringBuilder();
        char[] chars = text.toCharArray();
        for (char c : chars) {
            if (esc) {
                switch (c) {
                    case 'n':
                        buf.append('\n');
                        break;
                    case 't':
                        buf.append('\t');
                        break;
                    default:
                        buf.append(c);
                }
                esc = false;
                continue;
            }
            if (inString && c != '"') {
                buf.append(c);
                continue;
            }
            switch (c) {
                case ' ':
                case '\t':
                    if (buf.length() > 0) {
                        tokens.add(buf.toString());
                        buf.setLength(0);
                    }
                    break;
                case '\\':
                    esc = true;
                    break;
                case '"':
                    inString = !inString;
                    break;
                default:
                    buf.append(c);
                    break;
            }
        }
        if (buf.length() > 0) {
            tokens.add(buf.toString());
            buf.setLength(0);
        }
        return tokens.toArray(new String[tokens.size()]);
    }

    /**
     * Wildcard matches.
     */
    public static boolean matches(String pattern, String text) {
        // add sentinel so don't need to worry about *'s at end of pattern
        text += '\0';
        pattern += '\0';

        int N = pattern.length();

        boolean[] states = new boolean[N + 1];
        boolean[] old = new boolean[N + 1];
        old[0] = true;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            states = new boolean[N + 1];       // initialized to false
            for (int j = 0; j < N; j++) {
                char p = pattern.charAt(j);

                // hack to handle *'s that match 0 characters
                if (old[j] && (p == '*')) {
                    old[j + 1] = true;
                }

                if (old[j] && (p == c)) {
                    states[j + 1] = true;
                }
                if (old[j] && (p == '?')) {
                    states[j + 1] = true;
                }
                if (old[j] && (p == '*')) {
                    states[j] = true;
                }
                if (old[j] && (p == '*')) {
                    states[j + 1] = true;
                }
            }
            old = states;
        }
        return states[N];
    }

}
