/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
