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
 */
package org.nuxeo.ecm.automation.client.jaxrs.util;

/**
 * Base64 MIME content transfer encoding.
 *
 * @see http://en.wikipedia.org/wiki/Base64
 */
public class Base64 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    // Base-64 pad character "="
    private static final String PAD = "=";

    /**
     * Convert an array of big-endian words to a base-64 string
     */
    public static String encode(int[] arr, int byteLen) {
        return encode(toByteArray(arr, byteLen));
    }

    private static final int[] BSHIFT = { 24, 16, 8, 0 };

    protected static byte[] toByteArray(int[] arr, int byteLen) {
        byte[] bytes = new byte[byteLen];
        int l = Math.min(arr.length * 4, byteLen);
        int i = 0;
        for (int b = 0; b < l; b++) {
            bytes[b] = (byte) (arr[i] >> BSHIFT[b & 3] & 0xff);
        }
        return bytes;
    }

    public static String encode(String value) {
        return encode(stringToBytes(value));
    }

    public static String encode(byte[] arr) {
        StringBuilder sb = new StringBuilder();
        int l = arr.length;
        int m = l % 3;
        l -= m;
        for (int i = 0; i < l; i += 3) {
            encodeTriplet(sb, arr, i, 3);
        }
        if (m == 2) {
            encodeTriplet(sb, arr, l, 2);
        } else if (m == 1) {
            encodeTriplet(sb, arr, l, 1);
        }
        return sb.toString();
    }

    private static void encodeTriplet(StringBuilder sb, byte[] array,
            int index, int len) {
        int triplet = (array[index] & 0xFF) << 16;
        if (len >= 2)
            triplet |= (array[index + 1] & 0xFF) << 8;
        if (len >= 3)
            triplet |= (array[index + 2] & 0xFF);
        int pad = 3 - len;
        for (int j = 3; j >= pad; j--) {
            int p = (triplet >> (j * 6)) & 0x3F;
            sb.append(ALPHABET.charAt(p));
        }
        while (pad-- > 0)
            sb.append(PAD);
    }

    public static byte[] stringToBytes(String msg) {
        int len = msg.length();
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++)
            bytes[i] = (byte) (msg.charAt(i) & 0xff);
        return bytes;
    }
}
