/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
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
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ByteArrayBlob extends DefaultBlob implements Serializable {

    private static final long serialVersionUID = -91800812783051025L;

    protected final byte[] content;

    public ByteArrayBlob(byte[] content) {
        this(content, null, null);
    }

    public ByteArrayBlob(byte[] content, String ctype) {
        this(content, ctype, null);
    }

    public ByteArrayBlob(byte[] content, String ctype, String encoding) {
        this(content, ctype, encoding, null, null);
    }
    public ByteArrayBlob(byte[] content, String mimeType, String encoding,
            String filename, String digest) {
        this.content = content;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.filename = filename;
        this.digest = digest;
    }


    @Override
    public long getLength() {
        if (content == null || content.length == 0) {
            return 0;
        }
        return content.length;
    }


    @Override
    public InputStream getStream() throws IOException {
        if (content == null || content.length == 0) {
            return EMPTY_INPUT_STREAM;
        }
        return new ByteArrayInputStream(content);
    }

    @Override
    public byte[] getByteArray() throws IOException {
        if (content == null || content.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        return content;
    }

    @Override
    public String getString() throws IOException {
        if (content == null || content.length == 0) {
            return EMPTY_STRING;
        }
        return new String(content, encoding == null ? "UTF-8" : encoding);
    }

    @Override
    public Reader getReader() throws IOException {
        String str = getString();
        if (str == null || str.length() == 0) {
            return EMPTY_READER;
        }
        return new StringReader(str);
    }

    @Override
    public Blob persist() throws IOException {
        return this;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

}
