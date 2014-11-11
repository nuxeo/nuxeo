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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractBlob implements Blob {

    public static final String EMPTY_STRING = "";
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final InputStream EMPTY_INPUT_STREAM = new ByteArrayInputStream(EMPTY_BYTE_ARRAY);
    public static final Reader EMPTY_READER = new StringReader(EMPTY_STRING);

    protected static final int BUFFER_SIZE = 4096*16;
    //protected static int BUFFER_SIZE = 16;


    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static void copy(Reader in, Writer out) throws IOException {
        char[] buffer = new char[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    @Override
    public void transferTo(Writer writer) throws IOException {
        Reader reader = getReader();
        if (reader != null && reader != EMPTY_READER) {
            try {
                copy(reader, writer);
            } finally {
                reader.close();
            }
        }
    }

    @Override
    public void transferTo(OutputStream out) throws IOException {
        InputStream in = getStream();
        if (in != null && in != EMPTY_INPUT_STREAM) {
            try {
                copy(in, out);
            } finally {
                in.close();
            }
        }
    }

    @Override
    public void transferTo(File file) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            transferTo(out);
        } finally {
            if (out != null) {
                out.close();
            }
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
        if (digest != null && otherDigest != null
                && !digest.equals(otherDigest)) {
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
