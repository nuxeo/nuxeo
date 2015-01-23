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
package org.nuxeo.ecm.core.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A blob contains large binary data, and is associated with a MIME type, encoding, and filename. It also has a fixed
 * length, and a digest.
 * <p>
 * This interface requires that implementations of {@link #getStream} can be called several times, so the first call
 * must not "exhaust" the stream.
 */
public interface Blob {

    /**
     * Gets an {@link InputStream} for the data of this blob.
     * <p>
     * The contract of {@link Blob} is that this method can be called several times and will correctly return a new
     * {@link InputStream} each time. In other words, several reads of the {@link Blob} can be done.
     * <p>
     * Like all {@link InputStream}, the result must be closed when done with it to avoid resource leaks.
     *
     * @return the stream
     */
    InputStream getStream() throws IOException;

    /**
     * Gets the data length in bytes if known.
     *
     * @return the data length or -1 if not known
     */
    long getLength();

    String getMimeType();

    String getEncoding();

    String getFilename();

    String getDigest();

    void setMimeType(String mimeType);

    void setEncoding(String encoding);

    void setFilename(String filename);

    void setDigest(String digest);

    byte[] getByteArray() throws IOException;

    String getString() throws IOException;

    void transferTo(OutputStream out) throws IOException;

    void transferTo(File file) throws IOException;

    /**
     * If this blob is backed by an actual file, returns it.
     * <p>
     * The returned file may be short-lived (temporary), so should be used immediately.
     *
     * @return a file, or {@code null} if the blob is not backed by a file
     * @since 7.2
     */
    File getFile();

    /**
     * Gets a {@link CloseableFile} backing this blob, which must be closed when done by the caller.
     * <p>
     * The returned file may be the original file, a temporary file, or a symbolic link.
     *
     * @return a closeable file, to be closed when done
     * @since 7.2
     */
    CloseableFile getCloseableFile() throws IOException;

    /**
     * Gets a {@link CloseableFile} backing this blob, which must be closed when done by the caller.
     * <p>
     * The returned file may be the original file, a temporary file, or a symbolic link.
     *
     * @param ext the required extension for the file, or {@code null} if it doesn't matter
     * @return a closeable file, to be closed when done
     * @since 7.2
     */
    CloseableFile getCloseableFile(String ext) throws IOException;

}
