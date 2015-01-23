/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.impl.blob;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Blob based on a byte array.
 */
public class ByteArrayBlob extends AbstractBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final byte[] bytes;

    public ByteArrayBlob(byte[] bytes) {
        this(bytes, null, null);
    }

    public ByteArrayBlob(byte[] bytes, String mimeType) {
        this(bytes, mimeType, null);
    }

    public ByteArrayBlob(byte[] bytes, String mimeType, String encoding) {
        if (bytes == null) {
            throw new NullPointerException("null bytes");
        }
        this.bytes = bytes;
        this.mimeType = mimeType;
        this.encoding = encoding;
    }

    @Override
    public long getLength() {
        return bytes.length;
    }

    @Override
    public InputStream getStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public byte[] getByteArray() {
        return bytes;
    }

    @Override
    public String getString() throws IOException {
        return new String(bytes, getEncoding() == null ? UTF_8 : getEncoding());
    }

}
