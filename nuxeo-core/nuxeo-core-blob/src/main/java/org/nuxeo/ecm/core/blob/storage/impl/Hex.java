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

package org.nuxeo.ecm.core.blob.storage.impl;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Hex {

    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    public static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4 ];
            out[j++] = DIGITS[ 0x0F & data[i] ];
        }
        return out;
    }

    public static String toHexString(byte[] data) {
        int l = data.length;
        StringBuilder buf = new StringBuilder(l << 1);
        // two characters form the hex value.
        for (int i = 0; i < l; i++) {
            buf.append(DIGITS[(0xF0 & data[i]) >>> 4 ]);
            buf.append(DIGITS[ 0x0F & data[i] ]);
        }
        return buf.toString();
    }


}
