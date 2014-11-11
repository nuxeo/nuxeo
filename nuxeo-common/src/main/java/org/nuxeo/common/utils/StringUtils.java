/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: StringUtils.java 28482 2008-01-04 15:33:39Z sfermigier $
 */

package org.nuxeo.common.utils;

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
            + "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF"
            + "\u00C5\u00E5" + "\u00C7\u00E7";

    // This is an utility class.
    private StringUtils() {
    }

    /**
     * Replaces accented characters from a non-null String by their ascii
     * equivalent.
     */
    public static String toAscii(String s) {
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
     * Improved versions of join method from org.apache.commons.lang.StringUtils.
     */
    public static String join(Object[] array) {
        if (array == null) {
            return null;
        }
        int arraySize = array.length;
        int bufSize = arraySize == 0 ? 0
                : ((array[0] == null ? 16 : array[0].toString().length()) + 1) * arraySize;
        StringBuilder buf = new StringBuilder(bufSize);

        for (int i = 0; i < arraySize; i++) {
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }

    /**
     * Improved versions of join method from org.apache.commons.lang.StringUtils.
     */
    public static String join(Object[] array, String separator) {
        if (array == null) {
            return null;
        }
        int arraySize = array.length;
        if (arraySize == 0) {
            return "";
        }
        int bufSize = ((array[0] == null ? 16 : array[0].toString().length()) + 1) * arraySize;
        StringBuilder buf = new StringBuilder(bufSize);

        buf.append(array[0]);
        for (int i = 1; i < arraySize; i++) {
            if (separator != null) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }

    public static String join(Object[] array, char separator) {
        return join(array, String.valueOf(separator));
    }

    /**
     * Joins strings from a {@code List} with an optional separator.
     *
     * @param list the list.
     * @param separator the separator.
     * @return the joined string.
     */
    public static String join(List<String> list, String separator) {
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return "";
        }
        int seplen = (separator == null) ? 0 : separator.length();
        int len = -seplen;
        for (String s : list) {
            len += seplen;
            if (s != null) {
                len += s.length();
            }
        }
        StringBuilder buf = new StringBuilder(len);
        boolean first = true;
        for (String s : list) {
            if (first) {
                first = false;
            } else {
                if (seplen != 0) {
                    buf.append(separator);
                }
            }
            if (s != null) {
                buf.append(s);
            }
        }
        return buf.toString();
    }

    public static String join(List<String> list) {
        return join(list, null);
    }

    public static String join(List<String> list, char separator) {
        return join(list, String.valueOf(separator));
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

    public static String toHex(String string) {
        char[] chars = string.toCharArray();
        StringBuilder buf = new StringBuilder();
        for (char c : chars) {
            buf.append(Integer.toHexString(c).toUpperCase());
        }
        return buf.toString();
    }

    /**
     * Expands any variable found in the given expression with the values in the
     * given map.
     * <p>
     * The variable format is ${property_key}.
     *
     * @param expression the expression to expand
     * @param properties a map containing variables
     */
    public static String expandVars(String expression,
            Map<?, ?> properties) {
        int p = expression.indexOf("${");
        if (p == -1) {
            return expression; // do not expand if not needed
        }

        char[] buf = expression.toCharArray();
        StringBuilder result = new StringBuilder(buf.length);
        if (p > 0) {
            result.append(expression.substring(0, p));
        }
        StringBuilder varBuf = new StringBuilder();
        boolean dollar = false;
        boolean var = false;
        for (int i = p; i < buf.length; i++) {
            char c = buf[i];
            switch (c) {
            case '$' :
                dollar = true;
                break;
            case '{' :
                if (dollar) {
                    dollar = false;
                    var = true;
                } else {
                    result.append(c);
                }
                break;
            case '}':
                if (var) {
                    var = false;
                    String varName = varBuf.toString();
                    varBuf.setLength(0);
                    // get the variable value
                    Object varValue = properties.get(varName);
                    if (varValue != null) {
                        result.append(varValue.toString());
                    } else { // let the variable as is
                        result.append("${").append(varName).append('}');
                    }
                } else {
                    result.append(c);
                }
                break;
            default:
                if (var) {
                  varBuf.append(c);
                } else {
                    result.append(c);
                }
                break;
            }
        }
        return result.toString();
    }

}
