/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract implementation of a {@link Blob} storing the information other than the byte stream.
 */
public abstract class AbstractBlob implements Blob, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String UTF_8 = "UTF-8";

    public static final String TEXT_PLAIN = "text/plain";

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
    public String getDigestAlgorithm() {
        return null;
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
    public File getFile() {
        return null;
    }

    @Override
    public byte[] getByteArray() throws IOException {
        try (InputStream in = getStream()) {
            return IOUtils.toByteArray(in);
        }
    }

    @Override
    public String getString() throws IOException {
        try (Reader reader = new InputStreamReader(getStream(), getEncoding() == null ? UTF_8 : getEncoding())) {
            return IOUtils.toString(reader);
        }
    }

    @Override
    public long getLength() {
        return -1;
    }

    @Override
    public CloseableFile getCloseableFile() throws IOException {
        return getCloseableFile(null);
    }

    @Override
    public CloseableFile getCloseableFile(String ext) throws IOException {
        File file = getFile();
        if (file != null && (ext == null || file.getName().endsWith(ext))) {
            return new CloseableFile(file, false);
        }
        File tmp = Framework.createTempFile("nxblob-", ext);
        tmp.delete();
        if (file != null) {
            // attempt to create a symbolic link, which would be cheaper than a copy
            try {
                Files.createSymbolicLink(tmp.toPath(), file.toPath().toAbsolutePath());
            } catch (IOException | UnsupportedOperationException e) {
                // symbolic link not supported, do a copy instead
                Files.copy(file.toPath(), tmp.toPath());
            }
        } else {
            try (InputStream in = getStream()) {
                Files.copy(in, tmp.toPath());
            }
        }
        Framework.trackFile(tmp, tmp);
        return new CloseableFile(tmp, true);
    }

    @Override
    public void transferTo(OutputStream out) throws IOException {
        try (InputStream in = getStream()) {
            IOUtils.copy(in, out);
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
