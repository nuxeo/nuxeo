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
