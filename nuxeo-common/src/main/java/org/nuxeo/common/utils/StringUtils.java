/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.common.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utils for String manipulations.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class StringUtils {

    private static final String PLAIN_ASCII =
    // grave
    "AaEeIiOoUu"
            // acute
            + "AaEeIiOoUuYy"
            // circumflex
            + "AaEeIiOoUuYy"
            // tilde
            + "AaEeIiOoUuYy"
            // umlaut
            + "AaEeIiOoUuYy"
            // ring
            + "Aa"
            // cedilla
            + "Cc";

    private static final String UNICODE = "\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9"
            + "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD"
            + "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177"
            + "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177"
            + "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF" + "\u00C5\u00E5"
            + "\u00C7\u00E7";

    // This is an utility class.
    private StringUtils() {
    }

    /**
     * Replaces accented characters from a non-null String by their ascii equivalent.
     *
     * @param normalize if true, normalize the string using NFC
     * @since 7.1
     */
    public static String toAscii(String s, boolean normalize) {
        if (normalize) {
            s = Normalizer.normalize(s, Normalizer.Form.NFC);
        }
        StringBuilder sb = new StringBuilder();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            int pos = UNICODE.indexOf(c);
            if (pos > -1) {
                sb.append(PLAIN_ASCII.charAt(pos));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Replaces accented characters from a non-null String by their ascii equivalent.
     */
    public static String toAscii(String s) {
        return toAscii(s, false);
    }

    public static String[] split(String str, char delimiter, boolean trim) {
        int s = 0;
        int e = str.indexOf(delimiter, s);
        if (e == -1) {
            if (trim) {
                str = str.trim();
            }
            return new String[] { str };
        }
        List<String> ar = new ArrayList<>();
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

    /**
     * Expands any variable found in the given expression with the values in the given map.
     * <p>
     * The variable format is ${property_key}.
     *
     * @param expression the expression to expand
     * @param properties a map containing variables
     */
    public static String expandVars(String expression, Map<?, ?> properties) {
        return Vars.expand(expression, properties);
    }

}
