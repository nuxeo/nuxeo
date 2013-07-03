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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

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
    public void transferToOrMove(File file, boolean keep) throws IOException {
        if (this instanceof StreamingBlob) {
            StreamingBlob streamingBlob = (StreamingBlob) this;
            StreamSource streamSource = streamingBlob.getStreamSource();
            if (streamSource instanceof FileSource
                    && streamingBlob.isTemporary()) {
                FileSource fileSource = (FileSource) streamSource;
                atomicMove(fileSource.getFile(), file);
                fileSource.setFile(file);
                if (keep) {
                    // prevent further moves
                    streamingBlob.setTemporary(false);
                }
                return;
            }
        }
        // fallback to regular copy
        transferTo(file);
    }

    /**
     * Does an atomic move of the source file to the destination file.
     * <p>
     * Tries to work well with NFS mounts and different filesystems.
     *
     * @since 5.6.0-HF20, 5.7.2
     */
    public static void atomicMove(File source, File dest) throws IOException {
        if (dest.exists()) {
            // The file with the proper digest is already there so don't do
            // anything. This is to avoid "Stale NFS File Handle" problems
            // which would occur if we tried to overwrite it anyway.
            // Note that this doesn't try to protect from the case where
            // two identical files are uploaded at the same time.
            // Update date for the GC.
            dest.setLastModified(source.lastModified());
            return;
        }
        if (!source.renameTo(dest)) {
            // Failed to rename, probably a different filesystem.
            // Do *NOT* use Apache Commons IO's FileUtils.moveFile()
            // because it rewrites the destination file so is not atomic.
            // Do a copy through a tmp file on the same filesystem then
            // atomic rename.
            File tmp = File.createTempFile(dest.getName(), ".tmp",
                    dest.getParentFile());
            try {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = new FileInputStream(source);
                    out = new FileOutputStream(tmp);
                    IOUtils.copy(in, out);
                } finally {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                }
                // then do the atomic rename
                tmp.renameTo(dest);
            } finally {
                tmp.delete();
            }
            // finally remove the original source
            source.delete();
        }
        if (!dest.exists()) {
            throw new IOException("Could not create file: " + dest);
        }
    }

}
