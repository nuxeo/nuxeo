/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: DWord.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl;

import java.nio.ByteBuffer;

/**
 * An 32 bit integer that can handle bit operations.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DWord {

    private final ByteBuffer bb = ByteBuffer.allocate(4);

    public DWord() {
    }

    public DWord(int n) {
        bb.putInt(n);
    }

    public DWord(byte[] bytes) {
        bb.put(bytes);
    }

    public final void setInt(int n) {
        bb.putInt(n);
    }

    public void setBytes(byte[] bytes) {
        bb.put(bytes);
    }

    public final int getInt() {
        return bb.getInt(0);
    }

    public byte[] getBytes() {
        return bb.array();
    }

}
