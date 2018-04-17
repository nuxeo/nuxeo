/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.impl.blob;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.api.Framework;

/**
 * A {@link Blob} backed by a {@link File}.
 * <p>
 * The backing file may be in a temporary location, which is the case if this {@link FileBlob} was constructed from an
 * {@link InputStream} or from a file which was explicitly marked as temporary. In this case, the file may be renamed,
 * or the file location may be changed to a non-temporary one.
 */
public class FileBlob extends AbstractBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    protected File file;

    protected boolean isTemporary;

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
        if (file == null) {
            throw new NullPointerException("null file");
        }
        this.file = file;
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.digest = digest;
        this.filename = filename != null ? filename : file.getName();
    }

    /**
     * Creates a {@link FileBlob} from an {@link InputStream}, by saving it to a temporary file.
     * <p>
     * The input stream is closed.
     *
     * @param in the input stream, which is closed after use
     */
    public FileBlob(InputStream in) throws IOException {
        this(in, null, null);
    }

    /**
     * Creates a {@link FileBlob} from an {@link InputStream}, by saving it to a temporary file.
     * <p>
     * The input stream is closed.
     *
     * @param in the input stream, which is closed after use
     * @param mimeType the MIME type
     */
    public FileBlob(InputStream in, String mimeType) throws IOException {
        this(in, mimeType, null);
    }

    /**
     * Creates a {@link FileBlob} from an {@link InputStream}, by saving it to a temporary file.
     * <p>
     * The input stream is closed.
     *
     * @param in the input stream, which is closed after use
     * @param mimeType the MIME type
     * @param encoding the encoding
     */
    public FileBlob(InputStream in, String mimeType, String encoding) throws IOException {
        this(in, mimeType, encoding, null);
    }

    /**
     * Creates a {@link FileBlob} from an {@link InputStream}, by saving it to a temporary file.
     * <p>
     * The input stream is closed.
     *
     * @param in the input stream, which is closed after use
     * @param mimeType the MIME type
     * @param encoding the encoding
     * @param tmpDir the temporary directory for file creation
     */
    public FileBlob(InputStream in, String mimeType, String encoding, File tmpDir) throws IOException {
        if (in == null) {
            throw new NullPointerException("null inputstream");
        }
        this.mimeType = mimeType;
        this.encoding = encoding;
        isTemporary = true;
        try {
            file = File.createTempFile("nxblob-", ".tmp", tmpDir);
            Framework.trackFile(file, file);
            filename = file.getName();
            try (OutputStream out = new FileOutputStream(file)) {
                IOUtils.copy(in, out);
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Creates a {@link FileBlob} with an empty temporary file with the given extension.
     *
     * @param ext the temporary file extension
     * @return a file blob
     * @since 7.2
     */
    public FileBlob(String ext) throws IOException {
        isTemporary = true;
        file = Framework.createTempFile("nxblob-", ext);
        Framework.trackFile(file, file);
        filename = file.getName();
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public long getLength() {
        return file.length();
    }

    @Override
    public InputStream getStream() throws IOException {
        return new FileInputStream(file);
    }

    /**
     * Checks whether this {@link FileBlob} is backed by a temporary file.
     *
     * @since 7.2
     */
    public boolean isTemporary() {
        return isTemporary;
    }

    /**
     * Moves this blob's temporary file to a new non-temporary location.
     * <p>
     * The move is done as atomically as possible.
     *
     * @since 7.2
     */
    public void moveTo(File dest) throws IOException {
        if (!isTemporary) {
            throw new IOException("Cannot move non-temporary file: " + file);
        }
        Path path = file.toPath();
        Path destPath = dest.toPath();
        try {
            Files.move(path, destPath, ATOMIC_MOVE);
            file = dest;
        } catch (AtomicMoveNotSupportedException e) {
            // Do a copy through a tmp file on the same filesystem then atomic rename
            Path tmp = Files.createTempFile(destPath.getParent(), null, null);
            try {
                Files.copy(path, tmp, REPLACE_EXISTING);
                Files.delete(path);
                Files.move(tmp, destPath, ATOMIC_MOVE);
                file = dest;
            } catch (IOException ioe) {
                // don't leave tmp file in case of error
                Files.deleteIfExists(tmp);
                throw ioe;
            }
        }
        isTemporary = false;
    }

    public void setTemporary(boolean newValue) {
        isTemporary = newValue;
    }
}
