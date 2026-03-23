/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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

/**
 * RFC-2231 specifies how a MIME parameter value, like {@code Content-Disposition}'s {@code filename}, can be encoded to
 * contain arbitrary character sets.
 *
 * @author Florent Guillaume
 */
public class RFC2231 {

    // RFC 2231 attr-char: ALPHA / DIGIT / "!" / "#" / "$" / "&" / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~"
    private static final String ATTR_CHARS = "!#$&+-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ^_`abcdefghijklmnopqrstuvwxyz|~";

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    // Utility class
    private RFC2231() {
    }

    /**
     * @deprecated since 2025.18, not used anymore
     */
    @Deprecated(since = "2025.18", forRemoval = true)
    public static void percentEscape(StringBuilder sb, String value) {
        byte[] bytes = value.getBytes(UTF_8);
        for (byte b : bytes) {
            if (b < '+' || b == ';' || b == ',' || b == '\\' || b > 'z') {
                sb.append('%');
                String s = Integer.toHexString(b & 0xff).toUpperCase();
                if (s.length() < 2) {
                    sb.append('0');
                }
                sb.append(s);
            } else {
                sb.append((char) b);
            }
        }
    }

    /**
     * Encodes a value per RFC 2231, percent-encoding all non-attr-char bytes of the UTF-8 representation.
     *
     * @param sb the buffer to append the encoded value to
     * @param value the value to encode
     */
    protected static void encodeRFC2231(StringBuilder sb, String value) {
        byte[] bytes = value.getBytes(UTF_8);
        for (byte b : bytes) {
            int c = b & 0xff;
            if (ATTR_CHARS.indexOf(c) != -1) {
                sb.append((char) c);
            } else {
                sb.append('%');
                sb.append(HEX_DIGITS[c >> 4]);
                sb.append(HEX_DIGITS[c & 0xf]);
            }
        }
    }

    private static boolean needsEncoding(String value) {
        byte[] bytes = value.getBytes(UTF_8);
        for (byte b : bytes) {
            if (ATTR_CHARS.indexOf(b & 0xff) == -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Encodes a {@code Content-Disposition} header following RFC 6266 best practice. When encoding is needed, both
     * {@code filename} (raw fallback) and {@code filename*} (RFC 2231 encoded) parameters are included.
     *
     * @param filename the filename
     * @param inline {@code true} for an inline disposition, {@code false} for an attachment
     * @return a full string to set as value of a {@code Content-Disposition} header
     * @since 2025.18
     */
    public static String encodeContentDisposition(String filename, boolean inline) {
        var sb = new StringBuilder();
        sb.append(inline ? "inline" : "attachment");
        sb.append("; filename=").append(filename);
        if (needsEncoding(filename)) {
            sb.append("; filename*=UTF-8''");
            encodeRFC2231(sb, filename);
        }
        return sb.toString();
    }

    /**
     * @deprecated since 2025.18, use {@link #encodeContentDisposition(String, boolean)} instead
     */
    @Deprecated(since = "2025.18", forRemoval = true)
    public static String encodeContentDisposition(String filename, boolean inline, String userAgent) {
        return encodeContentDisposition(filename, inline);
    }

}
