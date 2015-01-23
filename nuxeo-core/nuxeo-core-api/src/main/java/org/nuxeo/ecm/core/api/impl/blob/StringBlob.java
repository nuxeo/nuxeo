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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Blob based on a string.
 */
public class StringBlob extends AbstractBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(StringBlob.class);

    protected final String string;

    public StringBlob(String content) {
        this(content, TEXT_PLAIN, UTF_8);
    }

    public StringBlob(String string, String mimeType) {
        this(string, mimeType, UTF_8);
    }

    public StringBlob(String string, String mimeType, String encoding) {
        if (string == null) {
            throw new NullPointerException("null string");
        }
        this.string = string;
        this.mimeType = mimeType;
        this.encoding = encoding;
    }

    @Override
    public long getLength() {
        try {
            return getByteArray().length;
        } catch (IOException e) {
            log.error("Error while getting byte array from blob, returning -1: " + getFilename());
            return -1;
        }
    }

    @Override
    public InputStream getStream() throws IOException {
        return new ByteArrayInputStream(getByteArray());
    }

    @Override
    public byte[] getByteArray() throws IOException {
        return string.getBytes(getEncoding() == null ? UTF_8 : getEncoding());
    }

    @Override
    public String getString() {
        return string;
    }

}
