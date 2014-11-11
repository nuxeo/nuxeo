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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StringBlob extends DefaultBlob implements Serializable {

    private static final long serialVersionUID = -1369527636846459436L;

    private static final Log log = LogFactory.getLog(StringBlob.class);

    protected final String content;

    public StringBlob(String content) {
        this(content, "text/plain", "UTF-8");
    }

    public StringBlob(String content, String mimeType) {
        this(content, mimeType, "UTF-8");
    }

    public StringBlob(String content, String mimeType, String encoding) {
        this.content = content;
        this.mimeType = mimeType;
        this.encoding = encoding;
    }

    @Override
    public long getLength() {
        if (content == null) {
            return 0;
        }
        try {
            return getByteArray().length;
        } catch (IOException e) {
            log.error(String.format(
                    "Error while getting byte array from blob %s, returning -1",
                    getFilename()));
            return -1;
        }
    }

    @Override
    public InputStream getStream() throws IOException {
        if (content == null || content.length() == 0) {
            return EMPTY_INPUT_STREAM;
        }
        return new ByteArrayInputStream(getByteArray());
    }

    @Override
    public byte[] getByteArray() throws IOException {
        if (content == null || content.length() == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        return content.getBytes(encoding == null ? "UTF-8" : encoding);
    }

    @Override
    public String getString() throws IOException {
        if (content == null || content.length() == 0) {
            return EMPTY_STRING;
        }
        return content;
    }

    @Override
    public Reader getReader() throws IOException {
        if (content == null || content.length() == 0) {
            return EMPTY_READER;
        }
        return new StringReader(content);
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public Blob persist() throws IOException {
        return this;
    }

}
