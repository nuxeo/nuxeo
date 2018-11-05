/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.blob.binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * A binary object that can be read, and has a length and a digest.
 *
 * @author Florent Guillaume
 * @author Bogdan Stefanescu
 */
public class Binary implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(Binary.class);

    protected final String digest;

    protected final String blobProviderId;

    protected transient File file;

    protected Binary(String digest, String blobProviderId) {
        this(null, digest, blobProviderId);
    }

    public Binary(File file, String digest, String blobProviderId) {
        this.file = file;
        this.digest = digest;
        this.blobProviderId = blobProviderId;
    }

    /**
     * Gets the digest algorithm from the digest length.
     *
     * @since 7.4
     */
    public String getDigestAlgorithm() {
        // Cannot use current digest algorithm of the binary manager here since it might have changed after the binary
        // storage
        String digest = getDigest();
        if (digest == null) {
            return null;
        }
        return AbstractBinaryManager.DIGESTS_BY_LENGTH.get(digest.length());
    }

    /**
     * Gets a string representation of the hex digest of the binary.
     *
     * @return the digest, characters are in the range {@code [0-9a-f]}
     */
    public String getDigest() {
        return digest;
    }

    /**
     * Gets the blob provider which created this blob.
     * <p>
     * This is usually the repository name.
     *
     * @return the blob provider id
     * @since 7.3
     */
    public String getBlobProviderId() {
        return blobProviderId;
    }

    /**
     * Gets an input stream for the binary.
     *
     * @return the input stream
     * @throws IOException
     */
    public InputStream getStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + digest + ')';
    }

    public File getFile() {
        return file;
    }

    private void writeObject(java.io.ObjectOutputStream oos) throws IOException, ClassNotFoundException {
        oos.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        file = recomputeFile();
    }

    /**
     * Recomputes the file attribute by getting it from a new Binary for the same digest.
     */
    protected File recomputeFile() {
        BlobManager bm = Framework.getService(BlobManager.class);
        BlobProvider bp = bm.getBlobProvider(blobProviderId);
        Binary binary = bp.getBinaryManager().getBinary(digest);
        if (binary == null) {
            log.error("Cannot fetch binary with digest " + digest);
            return null;
        }
        return binary.file;
    }

}
