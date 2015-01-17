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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractBlob implements Blob, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String UTF_8 = "UTF-8";

    public static final String TEXT_PLAIN = "text/plain";

    protected static final String EMPTY_STRING = "";

    protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    protected static final InputStream EMPTY_INPUT_STREAM = new ByteArrayInputStream(EMPTY_BYTE_ARRAY);

    protected static final Reader EMPTY_READER = new StringReader(EMPTY_STRING);

    protected static final int BUFFER_SIZE = 4096 * 16;

    protected String mimeType;

    protected String encoding;

    protected String filename;

    protected String digest;

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getDigest() {
        return digest;
    }

    @Override
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public void setDigest(String digest) {
        this.digest = digest;
    }

    @Override
    public byte[] getByteArray() throws IOException {
        try (InputStream in = getStream()) {
            if (in == null || in.available() == 0) {
                return EMPTY_BYTE_ARRAY;
            }
            return IOUtils.toByteArray(in);
        }
    }

    @Override
    public String getString() throws IOException {
        try (Reader reader = getReader()) {
            if (reader == null || reader == EMPTY_READER) {
                return EMPTY_STRING;
            }
            return IOUtils.toString(reader);
        }
    }

    @Override
    public Reader getReader() throws IOException {
        InputStream in = getStream();
        if (in == null || in.available() == 0) {
            return EMPTY_READER;
        }
        String enc = getEncoding();
        return enc == null ? new InputStreamReader(in) : new InputStreamReader(in, enc);
    }

    @Override
    public long getLength() {
        return -1;
    }

    @Override
    public void transferTo(Writer writer) throws IOException {
        try (Reader reader = getReader()) {
            if (reader != null) {
                IOUtils.copy(reader, writer);
            }
        }
    }

    @Override
    public void transferTo(OutputStream out) throws IOException {
        try (InputStream in = getStream()) {
            if (in != null) {
                IOUtils.copy(in, out);
            }
        }
    }

    @Override
    public void transferTo(File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            transferTo(out);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Blob)) {
            return false;
        }
        Blob other = (Blob) object;
        if (!ObjectUtils.equals(getFilename(), other.getFilename())) {
            return false;
        }
        if (!ObjectUtils.equals(getMimeType(), other.getMimeType())) {
            return false;
        }
        if (!ObjectUtils.equals(getEncoding(), other.getEncoding())) {
            return false;
        }
        // ignore null digests, they are sometimes lazily computed
        // therefore mutable
        String digest = getDigest();
        String otherDigest = other.getDigest();
        if (digest != null && otherDigest != null && !digest.equals(otherDigest)) {
            return false;
        }
        // compare streams
        return equalsStream(other);
    }

    // overridden by StorageBlob for improved performance
    protected boolean equalsStream(Blob other) {
        InputStream is = null;
        InputStream ois = null;
        try {
            persist();
            other.persist();
            is = getStream();
            ois = other.getStream();
            return IOUtils.contentEquals(is, ois);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(ois);
        }
    }

    // we don't implement a complex hashCode as we don't expect
    // to put blobs as hashmap keys
    @Override
    public int hashCode() {
        return new HashCodeBuilder() //
        .append(getFilename()) //
        .append(getMimeType()) //
        .append(getEncoding()) //
        .toHashCode();
    }

}
