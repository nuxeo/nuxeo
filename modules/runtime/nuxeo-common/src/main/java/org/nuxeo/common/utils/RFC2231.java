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
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.text.Normalizer;

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
     * @since 2025.20
     */
    private static String toAsciiFallback(String value) {
        // Keep a canonical UTF-8 decomposition
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        var sb = new StringBuilder(normalized.length() + 8);
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            int type = Character.getType(c);
            // strip any accentuation marker i.e. turns "café" into "cafe"
            if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                    || type == Character.ENCLOSING_MARK) {
                continue;
            }
            // Keep only printable ASCII; replace anything else (CTLs, non-Latin scripts, ...) with '_'
            if (c < 0x20 || c > 0x7E) {
                sb.append('_');
                continue;
            }
            // Escape backslashes and quotes
            if (c == '\\' || c == '"') {
                sb.append('\\');
            }
            sb.append(c);
        }
        // defaults to "file" if empty
        return sb.isEmpty() ? "file" : sb.toString();
    }

    /**
     * Encodes a {@code Content-Disposition} header following RFC 6266 best practice. When encoding is needed, both
     * {@code filename} (ASCII fallback per RFC 6266 Appendix D) and {@code filename*} (RFC 2231 / RFC 5987 encoded)
     * parameters are included. The fallback is a best-effort ASCII transliteration of the original filename, so legacy
     * user agents that ignore {@code filename*} still receive a usable name.
     * <p>
     * If the filename is {@code null} or blank, defaults to {@code "file"} to avoid NPEs and ensure valid headers.
     *
     * @param filename the filename, or {@code null}/{@code ""} to default to {@code "file"}
     * @param inline {@code true} for an inline disposition, {@code false} for an attachment
     * @return a full string to set as value of a {@code Content-Disposition} header
     * @since 2025.18
     */
    public static String encodeContentDisposition(String filename, boolean inline) {
        // Default to "file" if filename is null or blank, following DownloadServiceImpl pattern
        if (isBlank(filename)) {
            return (inline ? "inline" : "attachment") + "; filename=file";
        }
        var sb = new StringBuilder();
        sb.append(inline ? "inline" : "attachment");
        sb.append("; filename=");
        // Per RFC 6266, use quoted-string form and add a filename* parameter when the filename contains characters
        // that require RFC 2231 encoding (bytes outside the RFC 2231 / RFC 5987 attr-char set).
        if (needsEncoding(filename)) {
            String ascii = toAsciiFallback(filename);
            // Quote the ASCII fallback only when it contains non-token characters (spaces, parentheses, ...).
            if (needsEncoding(ascii)) {
                sb.append('"').append(ascii).append('"');
            } else {
                sb.append(ascii);
            }
            sb.append("; filename*=UTF-8''");
            encodeRFC2231(sb, filename);
        } else {
            sb.append(filename);
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
