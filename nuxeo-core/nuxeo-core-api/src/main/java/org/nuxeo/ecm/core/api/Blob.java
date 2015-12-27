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

    /**
     * @since 7.4
     */
    String getDigestAlgorithm();

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
