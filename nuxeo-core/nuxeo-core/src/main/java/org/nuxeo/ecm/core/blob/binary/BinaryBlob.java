/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob.binary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.AbstractBlob;
import org.nuxeo.ecm.core.blob.ManagedBlob;

/**
 * A {@link Blob} wrapping a {@link Binary} value.
 */
public class BinaryBlob extends AbstractBlob implements ManagedBlob, Serializable {

    private static final long serialVersionUID = 1L;

    protected final Binary binary;

    /**
     * The key, which is the binary's digest but may in addition be prefixed by a blob provider id.
     */
    protected final String key;

    protected final long length;

    public BinaryBlob(Binary binary, String key, String filename, String mimeType, String encoding, String digest,
            long length) {
        this.binary = binary;
        this.key = key;
        this.length = length;
        setFilename(filename);
        setMimeType(mimeType);
        setEncoding(encoding);
        setDigest(digest);
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public InputStream getStream() throws IOException {
        return binary.getStream();
    }

    /**
     * Gets the {@link Binary} attached to this blob.
     *
     * @since 5.9.4
     * @return the binary
     */
    public Binary getBinary() {
        return binary;
    }

    @Override
    public String getDigest() {
        String digest = super.getDigest();
        if (digest == null) {
            return binary.getDigest();
        } else {
            return digest;
        }
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getProviderId() {
        return binary.getBlobProviderId();
    }

    @Override
    public File getFile() {
        return binary.getFile();
    }

    /*
     * Optimized stream comparison method.
     */
    @Override
    protected boolean equalsStream(Blob blob) {
        if (!(blob instanceof BinaryBlob)) {
            return super.equalsStream(blob);
        }
        BinaryBlob other = (BinaryBlob) blob;
        if (binary == null) {
            return other.binary == null;
        } else if (other.binary == null) {
            return false;
        } else {
            return binary.getDigest().equals(other.binary.getDigest());
        }
    }

}
