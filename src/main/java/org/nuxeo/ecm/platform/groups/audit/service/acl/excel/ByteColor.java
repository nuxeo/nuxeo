/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.excel;

public class ByteColor {
    public static final ByteColor BLACK = new ByteColor((byte) 0x00, (byte) 0x00, (byte) 0x00);

    public static final ByteColor WHITE = new ByteColor((byte) 0xFF, (byte) 0xFF, (byte) 0xFF);

    public static final ByteColor GREEN = new ByteColor((byte) 0x00, (byte) 0xFF, (byte) 0x00);

    public static final ByteColor RED = new ByteColor((byte) 0xFF, (byte) 0x00, (byte) 0x00);

    public static final ByteColor BLUE = new ByteColor((byte) 0x00, (byte) 0x00, (byte) 0xFF);

    public ByteColor(byte r, byte g, byte b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public byte r;

    public byte g;

    public byte b;
}
