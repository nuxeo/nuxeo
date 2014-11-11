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
 * $Id$
 */

package org.nuxeo.ecm.platform.picture;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper to handle the UNDEFINED Exif data type.
 * <p>
 * See {@link http
 * ://www.leadtools.com/help/leadtools/v15/Main/API/Dllaux/ExifComments.htm}.
 *
 * @author btatar
 */
public class ExifHelper {

    public static final Log log = LogFactory.getLog(ExifHelper.class);

    // the ASCII data format
    public static final byte[] ASCII = { 65, 83, 67, 73, 73, 0, 0, 0 };

    // the JIS data format
    public static final byte[] JIS = { 74, 73, 83, 0, 0, 0, 0, 0 };

    // the UNDEFINED data format
    public static final byte[] UNDEFINED = { 0, 0, 0, 0, 0, 0, 0, 0 };

    private ExifHelper() {
    }

    /**
     * Method used to perform the decode of the <b>Exif User comment</b> data
     * type. The first eight bytes specify the data format, and the remainder of
     * the comment is in the specified format.The first eight bytes can be any
     * of the following cases: 65, 83, 67, 73, 73, 0, 0, 0 = ASCII 74, 73, 83,
     * 0, 0, 0, 0, 0 = JIS 0, 0, 0, 0, 0, 0, 0, 0 = UNDEFINED
     *
     * @param rawBytes the user comment represented as a byte array
     * @return the user comment as a String on the format retrieved from the
     *         data type.
     */
    public static String decodeUndefined(byte[] rawBytes) {

        byte[] dataType = extractBytes(rawBytes, 0, 8);
        if (Arrays.equals(ASCII, dataType)) {
            if (rawBytes.length <= 8) {
                return "";
            }
            return new String(extractBytes(rawBytes, 8, rawBytes.length - 1));
        } else if (Arrays.equals(JIS, dataType)) {
            log.warn("The Japanese data type encoding is not supported yet");
            return "";
        } else if (Arrays.equals(UNDEFINED, dataType)) {
            log.debug("Undefined data type encoding");
            return "";
        } else {
            log.debug("Unknown data type encoding");
            return "";
        }
    }

    /**
     * Extracts the bytes from the received byte array. The first argument
     * represents the starting location (zero-based) and the second argument
     * represent the ending location which is not zero based.
     *
     * @param bytes the byte array
     * @param beginIndex the begin index which is zero based
     * @param endIndex the end index which is not zero based
     */
    public static byte[] extractBytes(byte[] bytes, int beginIndex, int endIndex) {
        byte[] result = new byte[endIndex - beginIndex];
        int count = 0;
        for (int i = beginIndex; i < endIndex; i++) {
            result[count++] = bytes[i];
        }
        return result;
    }

}
