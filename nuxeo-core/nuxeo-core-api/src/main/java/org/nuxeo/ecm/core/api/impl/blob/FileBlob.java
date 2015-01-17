/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api.impl.blob;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileBlob extends AbstractBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    protected transient File file;

    public FileBlob(File file) {
        this(file, null, null, null, null);
    }

    public FileBlob(File file, String mimeType) {
        this(file, mimeType, null, null, null);
    }

    public FileBlob(File file, String mimeType, String encoding) {
        this(file, mimeType, encoding, null, null);
    }

    public FileBlob(File file, String mimeType, String encoding, String filename, String digest) {
        this.file = file;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.digest = digest;
        this.filename = filename != null ? filename : file != null ? file.getName() : null;
    }

    public FileBlob(InputStream in) throws IOException {
        this(in, null, null);
    }

    public FileBlob(InputStream in, String ctype) throws IOException {
        this(in, ctype, null);
    }

    public FileBlob(InputStream in, String mimeType, String encoding) throws IOException {
        this.mimeType = mimeType;
        this.encoding = encoding;
        OutputStream out = null;
        try {
            file = File.createTempFile("NXCore-FileBlob-", ".tmp");
            out = new FileOutputStream(file);
            IOUtils.copy(in, out);
            Framework.trackFile(file, this);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    public File getFile() {
        return file;
    }

    @Override
    public long getLength() {
        return file == null ? 0L : file.length();
    }

    @Override
    public InputStream getStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
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
