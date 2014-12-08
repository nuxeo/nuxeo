/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.excel;

public class ByteColor {
    public static ByteColor BLACK = new ByteColor((byte) 0x00, (byte) 0x00, (byte) 0x00);

    public static ByteColor WHITE = new ByteColor((byte) 0xFF, (byte) 0xFF, (byte) 0xFF);

    public static ByteColor GREEN = new ByteColor((byte) 0x00, (byte) 0xFF, (byte) 0x00);

    public static ByteColor RED = new ByteColor((byte) 0xFF, (byte) 0x00, (byte) 0x00);

    public static ByteColor BLUE = new ByteColor((byte) 0x00, (byte) 0x00, (byte) 0xFF);

    public ByteColor(byte r, byte g, byte b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public byte r;

    public byte g;

    public byte b;
}
