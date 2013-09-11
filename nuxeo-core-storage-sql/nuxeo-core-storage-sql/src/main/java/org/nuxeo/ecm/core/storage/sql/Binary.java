/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

/**
 * A binary object that can be read, and has a length and a digest.
 *
 * @author Florent Guillaume
 * @author Bogdan Stefanescu
 */
public class Binary implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String digest;

    protected final String repoName;

    protected transient File file;

    protected long length;

    public Binary(File file, String digest) {
        this(file, digest, null);
    }

    protected Binary(String digest, String reponame) {
        this(null, digest, reponame);
    }

    public Binary(File file, String digest, String repoName) {
        this.file = file;
        this.digest = digest;
        this.repoName = repoName;
        length = -1;
    }

    /**
     * Compute length on demand, default implementation only works if
     * the file referenced contains the binary original content. If you're contributing
     * a binary type, you should adapt this in case you're encoding the content.
     *
     * This method is only used when users make a direct access to the binary.
     * Persisted blobs don't use that API.
     *
     * @since 5.7.3
     */
    protected long computeLength() {
        if (file == null) {
            return -1;
        }
        return file.length();
    }

    /**
     * Gets the length of the binary.
     *
     * @return the length of the binary
     */
    public long getLength() {
        if (length == -1) {
            length = computeLength();
        }
        return length;
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

    public StreamSource getStreamSource() {
        return new FileSource(file);
    }

    private void writeObject(java.io.ObjectOutputStream oos)
            throws IOException, ClassNotFoundException {
        oos.defaultWriteObject();
        if (repoName == null) {
            oos.writeObject(file);
        }
    }

    private void readObject(java.io.ObjectInputStream ois) throws IOException,
            ClassNotFoundException {
        ois.defaultReadObject();
        file = repoName == null ? (File) ois.readObject() : fetchData();
    }

    protected File fetchData() {
        BinaryManager mgr = RepositoryResolver.getBinaryManager(repoName);
        return mgr.getBinary(digest).file;
    }

}
