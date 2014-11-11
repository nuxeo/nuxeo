/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
 * or jersey/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at jersey/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.jboss.resteasy.specimpl.patch;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

/**
 * Utility class for validating, encoding and decoding components
 * of a URI.
 *
 * TODO rewrite to use masks and not lookup tables
 *
 * @author Paul.Sandoz@Sun.Com
 */
public final class UriComponent {

    public enum Type {
        SCHEME, USER_INFO, HOST, PORT, PATH, PATH_SEGMENT, QUERY, FRAGMENT,
    }

    private UriComponent() {
    }

    /**
     * Validates the legal characters of a percent-encoded string that
     * represents a URI component type.
     *
     * @param s the encoded string.
     * @param t the URI compontent type identifying the legal characters.
     * @throws IllegalArgumentException if the encoded string contains illegal
     *         characters.
     */
    public static void validate(String s, Type t) {
        validate(s, t, false);
    }

    /**
     * Validates the legal characters of a percent-encoded string that
     * represents a URI component type.
     *
     * @param s the encoded string.
     * @param t the URI compontent type identifying the legal characters.
     * @param template true if the encoded string contains URI template variables
     * @throws IllegalArgumentException if the encoded string contains illegal
     *         characters.
     */
    public static void validate(String s, Type t, boolean template) {
        int i = _valid(s, t, template);
        if (i > -1)
            // TODO localize
            throw new IllegalArgumentException("The string '" + s +
                    "' for the URI component " + t +
                    " contains an invalid character, '" + s.charAt(i) + "', at index " + i);
    }

    /**
     * Validates the legal characters of a percent-encoded string that
     * represents a URI component type.
     *
     * @param s the encoded string.
     * @param t the URI compontent type identifying the legal characters.
     * @return true if the encoded string is valid, otherwise false.
     */
    public static boolean valid(String s, Type t) {
        return valid(s, t, false);
    }

    /**
     * Validates the legal characters of a percent-encoded string that
     * represents a URI component type.
     *
     * @param s the encoded string.
     * @param t the URI compontent type identifying the legal characters.
     * @param template true if the encoded string contains URI template variables
     * @return true if the encoded string is valid, otherwise false.
     */
    public static boolean valid(String s, Type t, boolean template) {
        return _valid(s, t, template) == -1;
    }

    private static int _valid(String s, Type t, boolean template) {
        boolean[] table = ENCODING_TABLES[t.ordinal()];

        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if ((c < 0x80 && c != '%' && !table[c]) || c >= 0x80)
                if (!template || (c != '{' && c != '}'))
                    return i;
        }
        return -1;
    }

    /**
     * Encodes the characters of string that are either non-ASCII characters
     * or are ASCII characters that must be percent-encoded using the
     * UTF-8 encoding.
     *
     * @param s the string to be encoded.
     * @param t the URI compontent type identifying the ASCII characters that
     *          must be percent-encoded.
     * @return the encoded string.
     */
    public static String encode(String s, Type t) {
        return encode(s, t, false);
    }

    /**
     * Encodes the characters of string that are either non-ASCII characters
     * or are ASCII characters that must be percent-encoded using the
     * UTF-8 encoding.
     *
     * @param s the string to be encoded.
     * @param t the URI compontent type identifying the ASCII characters that
     *          must be percent-encoded.
     * @param template true if the encoded string contains URI template variables
     * @return the encoded string.
     */
    public static String encode(String s, Type t, boolean template) {
        boolean[] table = ENCODING_TABLES[t.ordinal()];

        StringBuilder sb = null;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c < 0x80 && table[c]) {
                if (sb != null) sb.append(c);
            } else {
                if (template && (c == '{' || c == '}')) {
                    if (sb != null) sb.append(c);
                    continue;
                }

                if (sb == null) {
                    sb = new StringBuilder();
                    sb.append(s.substring(0, i));
                }

                if (c < 0x80)
                    appendPercentEncodedOctet(sb, c);
                else
                    appendUTF8EncodedCharacter(sb, c);
            }
        }

        return (sb == null) ? s : sb.toString();
    }

    private final static char[] HEX_DIGITS = {
	'0', '1', '2', '3', '4', '5', '6', '7',
	'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static void appendPercentEncodedOctet(StringBuilder sb, int b) {
	sb.append('%');
	sb.append(HEX_DIGITS[b >> 4]);
	sb.append(HEX_DIGITS[b & 0x0F]);
    }

    private static void appendUTF8EncodedCharacter(StringBuilder sb, char c) {
        final ByteBuffer bb = UTF_8_CHARSET.encode("" + c);

	while (bb.hasRemaining()) {
            appendPercentEncodedOctet(sb, bb.get() & 0xFF);
	}
    }

    private static final String[] SCHEME = {"0-9", "A-Z", "a-z", "+", "-", "."};

    private static final String[] UNRESERVED = {"0-9", "A-Z", "a-z", "-", ".", "_", "~"};

    private static final String[] SUB_DELIMS = {"!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "="};

    private static final boolean[][] ENCODING_TABLES = creatingEncodingTables();

    private static boolean[][] creatingEncodingTables() {
        boolean[][] tables = new boolean[Type.values().length][];

        List<String> l = new ArrayList<String>();
        l.addAll(Arrays.asList(SCHEME));
        tables[Type.SCHEME.ordinal()] = creatingEncodingTable(l);

        l.clear();
        l.addAll(Arrays.asList(UNRESERVED));
        l.addAll(Arrays.asList(SUB_DELIMS));

        tables[Type.HOST.ordinal()] = creatingEncodingTable(l);

        tables[Type.PORT.ordinal()] = creatingEncodingTable(Arrays.asList("0-9"));

        l.add(":");

        tables[Type.USER_INFO.ordinal()] = creatingEncodingTable(l);

        l.add("@");

        tables[Type.PATH_SEGMENT.ordinal()] = creatingEncodingTable(l);

        l.add("/");

        tables[Type.PATH.ordinal()] = creatingEncodingTable(l);

        l.add("?");

        tables[Type.QUERY.ordinal()] = creatingEncodingTable(l);
        tables[Type.FRAGMENT.ordinal()] = tables[Type.QUERY.ordinal()];

        return tables;
    }

    private static boolean[] creatingEncodingTable(List<String> allowed) {
        boolean[] table = new boolean[0x80];
        for (String range : allowed) {
            if (range.length() == 1)
                table[range.charAt(0)] = true;
            else if (range.length() == 3 && range.charAt(1) == '-')
                for (int i = range.charAt(0); i <= range.charAt(2); i++)
                    table[i] = true;
        }

        return table;
    }

    private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

    /**
     * Decodes characters of a string that are percent-encoded octets using
     * UTF-8 decoding (if needed).
     * <p>
     * It is assumed that the string is valid according to an (unspecified) URI
     * component type. If a sequence of contiguous percent-encoded octets is
     * not a valid UTF-8 character then the octets are replaced with '\uFFFD'.
     * <p>
     * If the URI component is of type HOST then any "%" found between "[]" is
     * left alone. It is an IPv6 literal with a scope_id.
     * <p>
     * @param s the string to be decoded.
     * @param t the URI component type, may be null.
     * @return the decoded string.
     * @throws IllegalArgumentException if a malformed percent-encoded octet is
     *         detected
     */
    public static String decode(String s, Type t) {
	if (s == null)
	    throw new IllegalArgumentException();

	final int n = s.length();
	if (n == 0)
	    return s;

        // If there are no percent-escaped octets
	if (s.indexOf('%') < 0)
	    return s;

        // Malformed percent-escaped octet at the end
        if (n < 2)
            // TODO localize
	    throw new IllegalArgumentException("Malformed percent-encoded octet at index 1");

        // Malformed percent-escaped octet at the end
        if (s.charAt(n - 2) == '%')
            // TODO localize
	    throw new IllegalArgumentException("Malformed percent-encoded octet at index " + (n - 2));

        return (t != Type.HOST) ? decode(s, n) : decodeHost(s, n);
    }

    /**
     * Decode the query component of a URI.
     *
     * @param u the URI.
     * @param decode true of the query parameters of the query component
     *        should be in decoded form.
     * @return the multivalued map of query parameters.
     */
    public static MultivaluedMap<String, String> decodeQuery(URI u, boolean decode) {
        return decodeQuery(u.getRawQuery(), decode);
    }

    /**
     * Decode the query component of a URI.
     * <p>
     * TODO the implementation is not very efficient.
     *
     * @param q the query component in encoded form.
     * @param decode true of the query parameters of the query component
     *        should be in decoded form.
     * @return the multivalued map of query parameters.
     */
    public static MultivaluedMap<String, String> decodeQuery(String q, boolean decode) {
        MultivaluedMap<String, String> queryParameters = new MultivaluedMapImpl<String,String>();

        if (q == null || q.length() == 0)
            return queryParameters;

        for (String s : q.split("&")) {
            if (s.length() == 0)
                continue;

            String[] keyVal = s.split("=");
            try {
                String key = (decode) ? URLDecoder.decode(keyVal[0], "UTF-8") : keyVal[0];
                if (key.length() == 0)
                    continue;

                // Query parameter may not have a value, if so default to "";
                String val = (keyVal.length == 2) ?
                    (decode) ? URLDecoder.decode(keyVal[1], "UTF-8") : keyVal[1] : "";

                queryParameters.add(key, val);
            } catch (UnsupportedEncodingException ex) {
                // This should never occur
                throw new IllegalArgumentException(ex);
            }
        }

        return queryParameters;
    }

    private static String decode(String s, int n) {
	final StringBuilder sb = new StringBuilder(n);
	ByteBuffer bb = ByteBuffer.allocate(1);

	for (int i = 0; i < n;) {
            final char c = s.charAt(i++);
	    if (c != '%') {
		sb.append(c);
	    } else {
                bb = decodePercentEncodedOctets(s, i, bb);
                i = decodeOctets(i, bb, sb);
            }
	}

	return sb.toString();
    }

    private static String decodeHost(String s, int n) {
	final StringBuilder sb = new StringBuilder(n);
	ByteBuffer bb = ByteBuffer.allocate(1);

    	boolean betweenBrackets = false;
	for (int i = 0; i < n;) {
            final char c = s.charAt(i++);
	    if (c == '[') {
		betweenBrackets = true;
	    } else if (betweenBrackets && c == ']') {
		betweenBrackets = false;
	    }

	    if (c != '%' || betweenBrackets) {
		sb.append(c);
	    } else {
                bb = decodePercentEncodedOctets(s, i, bb);
                i = decodeOctets(i, bb, sb);
            }
	}

	return sb.toString();
    }

    /**
     * Decode a contigious sequence of percent encoded octets.
     * <p>
     * Assumes the index, i, starts that the first hex digit of the first
     * percent-encoded octet.
     */
    private static ByteBuffer decodePercentEncodedOctets(String s, int i, ByteBuffer bb) {
        bb.clear();

        while (true) {
            // Decode the hex digits
            bb.put((byte) (decodeHex(s, i++) << 4 | decodeHex(s, i++)));

            // Finish if at the end of the string
            if (i == s.length())
                break;

            // Finish if no more percent-encoded octets follow
            if (s.charAt(i++) != '%')
                break;

            // Check if the byte buffer needs to be increased in size
            if (bb.position() == bb.capacity()) {
                bb.flip();
                // Create a new byte buffer with the maximum number of possible
                // octets, hence resize should only occur once
                ByteBuffer bb_new = ByteBuffer.allocate(s.length() / 3);
                bb_new.put(bb);
                bb = bb_new;
            }
        }

        bb.flip();
        return bb;
    }

    /**
     * Decodes octets to characters using the UTF-8 decoding and appends
     * the characters to a StringBuffer.
     * @return the index to the next unchecked character in the string to decode
     */
    private static int decodeOctets(int i, ByteBuffer bb, StringBuilder sb) {
        // If there is only one octet and is an ASCII character
        if (bb.limit() == 1 && (bb.get(0) & 0xFF) < 0x80) {
            // Octet can be appended directly
            sb.append((char)bb.get(0));
            return i + 2;
        } else {
            //
            CharBuffer cb = UTF_8_CHARSET.decode(bb);
            sb.append(cb.toString());
            return i + bb.limit() * 3 - 1;
        }
    }

    private static int decodeHex(String s, int i) {
        final int v = decodeHex(s.charAt(i));
        if (v == -1)
            // TODO localize
            throw new IllegalArgumentException("Malformed percent-encoded octet at index " + i +
                    ", invalid hexadecimal digit '" + s.charAt(i) + "'");
        return v;
    }

    private static final int[] HEX_TABLE = createHexTable();

    private static int[] createHexTable() {
        int[] table = new int[0x80];
        Arrays.fill(table, -1);

        for (char c = '0'; c <= '9'; c++) table[c] = c - '0';
        for (char c = 'A'; c <= 'F'; c++) table[c] = c - 'A' + 10;
        for (char c = 'a'; c <= 'f'; c++) table[c] = c - 'a' + 10;
        return table;
    }

    private static int decodeHex(char c) {
        return (c < 128) ? HEX_TABLE[c] : -1;
    }
}