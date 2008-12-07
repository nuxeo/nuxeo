/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import java.io.UnsupportedEncodingException;

/**
 * RFC-2231 specifies how a MIME parameter value, like {@code
 * Content-Disposition}'s {@code filename}, can be encoded to contain arbitrary
 * character sets.
 *
 * @author Florent Guillaume
 */
public class RFC2231 {

    private static final String UTF8 = "UTF-8";

    private static final byte[] UNKNOWN_BYTES = { '?' };

    // Utility class
    private RFC2231() {
    }

    /**
     * Does a simple %-escaping of the UTF-8 bytes of the value. Keep only some
     * know safe characters.
     *
     * @param buf the buffer to which escaped chars are appended
     * @param value the value to escape
     */
    public static void percentEscape(StringBuilder buf, String value) {
        byte[] bytes;
        try {
            bytes = value.getBytes(UTF8);
        } catch (UnsupportedEncodingException e) {
            // cannot happen with UTF-8
            bytes = UNKNOWN_BYTES;
        }
        for (byte b : bytes) {
            if (b < '+' || b == ';' || b == '\\' || b > 'z') {
                buf.append('%');
                String s = Integer.toHexString(b & 0xff).toUpperCase();
                if (s.length() < 2) {
                    buf.append('0');
                }
                buf.append(s);
            } else {
                buf.append((char) b);
            }
        }
    }

    /**
     * Encodes a {@code Content-Disposition} header. For some user agents the
     * full RFC-2231 encoding won't be performed as they don't understand it.
     *
     * @param filename the filename
     * @param inline {@code true} for an inline disposition, {@code false} for
     *            an attachment
     * @param userAgent the userAgent
     * @return a full string to set as value of a {@code Content-Disposition}
     *         header
     */
    public static String encodeContentDisposition(String filename,
            boolean inline, String userAgent) {
        StringBuilder buf = new StringBuilder(inline ? "inline; "
                : "attachment; ");
        if (userAgent == null) {
            userAgent = "";
        }
        if (userAgent.contains("Firefox")) {
            // proper RFC2231
            buf.append("filename*=UTF-8''");
            percentEscape(buf, filename);
        } else {
            buf.append("filename=");
            if (userAgent.contains("MSIE")) {
                // MSIE understands straight %-encoding
                percentEscape(buf, filename);
            } else {
                // Safari (maybe others) expects direct UTF-8 encoded strings
                buf.append(filename);
            }
        }
        return buf.append(';').toString();
    }

}
