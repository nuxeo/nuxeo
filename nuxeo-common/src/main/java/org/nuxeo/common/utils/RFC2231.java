/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.common.utils.UserAgentMatcher.isMSIE6or7;

/**
 * RFC-2231 specifies how a MIME parameter value, like {@code Content-Disposition}'s {@code filename}, can be encoded to
 * contain arbitrary character sets.
 *
 * @author Florent Guillaume
 */
public class RFC2231 {

    // RFC 2045
    private static final String MIME_SPECIALS = "()<>@,;:\\\"/[]?=\t ";

    // RFC 2231
    private static final String RFC2231_SPECIALS = "*'%" + MIME_SPECIALS;

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    // Utility class
    private RFC2231() {
    }

    /**
     * Does a simple %-escaping of the UTF-8 bytes of the value. Keep only some know safe characters.
     *
     * @param buf the buffer to which escaped chars are appended
     * @param value the value to escape
     */
    public static void percentEscape(StringBuilder buf, String value) {
        byte[] bytes = value.getBytes(UTF_8);
        for (byte b : bytes) {
            if (b < '+' || b == ';' || b == ',' || b == '\\' || b > 'z') {
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
     * Encodes a MIME parameter per RFC 2231.
     * <p>
     * This implementation always uses UTF-8 and no language.
     *
     * @param buf the buffer to fill
     * @param value the value to encode
     */
    protected static void encodeRFC2231(StringBuilder buf, String value) {
        int originalLength = buf.length();
        buf.append("*=UTF-8''"); // no language
        byte[] bytes = value.getBytes(UTF_8);
        boolean encoded = false;
        for (int i = 0; i < bytes.length; i++) {
            int c = bytes[i] & 0xff;
            if (c <= 32 || c >= 127 || RFC2231_SPECIALS.indexOf(c) != -1) {
                buf.append('%');
                buf.append(HEX_DIGITS[c >> 4]);
                buf.append(HEX_DIGITS[c & 0xf]);
                encoded = true;
            } else {
                buf.append((char) c);
            }
        }
        if (!encoded) {
            // undo and use basic format
            buf.setLength(originalLength);
            buf.append('=');
            buf.append(value);
        }
    }

    /**
     * Encodes a {@code Content-Disposition} header. For some user agents the full RFC-2231 encoding won't be performed
     * as they don't understand it.
     *
     * @param filename the filename
     * @param inline {@code true} for an inline disposition, {@code false} for an attachment
     * @param userAgent the userAgent
     * @return a full string to set as value of a {@code Content-Disposition} header
     */
    public static String encodeContentDisposition(String filename, boolean inline, String userAgent) {
        StringBuilder buf = new StringBuilder();
        buf.append(inline ? "inline" : "attachment");
        buf.append("; filename");
        if (userAgent == null) {
            userAgent = "";
        }
        if (isMSIE6or7(userAgent)) {
            // MSIE understands straight %-encoding
            buf.append("=");
            percentEscape(buf, filename);
        } else {
            // proper RFC2231
            encodeRFC2231(buf, filename);
        }
        return buf.toString();
    }

}
