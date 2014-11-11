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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.api.Framework;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileBlob extends DefaultStreamBlob implements Serializable {

    private static final long serialVersionUID = 373720300515677319L;

    protected transient File file;

    public FileBlob(File file) {
        this(file, null, null);
    }

    public FileBlob(File file, String ctype) {
        this(file, ctype, null);
    }

    public FileBlob(File file, String mimeType, String encoding) {
        this (file, mimeType, encoding, null, null);
    }

    public FileBlob(File file, String mimeType, String encoding, String filename, String digest) {
        this.file = file;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.digest = digest;
        this.filename = filename != null ? filename
                : file != null ? file.getName() : null;
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
            file.deleteOnExit();
            out = new FileOutputStream(file);
            copy(in, out);
            Framework.trackFile(file, this);
        } finally {
            FileUtils.close(in);
            FileUtils.close(out);
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

    private void readObject(ObjectInputStream in)
            throws ClassNotFoundException, IOException {
        // always perform the default de-serialization first
        in.defaultReadObject();
        // create a temp file where we will put the blob content
        file = File.createTempFile("NXCore-FileBlob-", ".tmp");
        file.deleteOnExit();
        Framework.trackFile(file, this);
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            int bytes = in.readInt();
            while (bytes > -1 && (read = in.read(buffer, 0, bytes)) != -1) {
                out.write(buffer, 0, read);
                bytes -= read;
                if (bytes == 0) {
                    bytes = in.readInt();
                }
            }
        } finally {
            FileUtils.close(out);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        // write content
        InputStream in = null;
        try {
            in = getStream();
            int read = 0;
            byte[] buf = new byte[BUFFER_SIZE];
            while ((read = in.read(buf)) != -1) {
                out.writeInt(read); // next follows a chunk of 'read' bytes
                out.write(buf, 0, read);
            }
            out.writeInt(-1); // EOF
        } finally {
            FileUtils.close(in);
        }

    }

}
