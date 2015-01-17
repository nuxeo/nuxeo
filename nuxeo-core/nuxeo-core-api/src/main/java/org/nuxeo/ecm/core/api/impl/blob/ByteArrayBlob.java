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
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;

import org.nuxeo.ecm.core.api.Blob;

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
        this(bytes, mimeType, encoding, null, null);
    }

    public ByteArrayBlob(byte[] bytes, String mimeType, String encoding, String filename, String digest) {
        this.bytes = bytes;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.filename = filename;
        this.digest = digest;
    }

    @Override
    public long getLength() {
        if (bytes == null) {
            return 0;
        }
        return bytes.length;
    }

    @Override
    public InputStream getStream() {
        if (bytes == null) {
            return EMPTY_INPUT_STREAM;
        }
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public byte[] getByteArray() {
        if (bytes == null) {
            return EMPTY_BYTE_ARRAY;
        }
        return bytes;
    }

    @Override
    public String getString() throws IOException {
        if (bytes == null) {
            return EMPTY_STRING;
        }
        return new String(bytes, encoding == null ? "UTF-8" : encoding);
    }

    @Override
    public Reader getReader() throws IOException {
        return new StringReader(getString());
    }

    @Override
    public Blob persist() {
        return this;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

}
