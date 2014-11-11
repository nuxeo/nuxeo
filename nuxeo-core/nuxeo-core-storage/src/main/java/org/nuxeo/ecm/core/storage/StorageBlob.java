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
package org.nuxeo.ecm.core.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.DefaultStreamBlob;
import org.nuxeo.ecm.core.storage.binary.Binary;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;

/**
 * A {@link Blob} wrapping a {@link Binary} value.
 */
public class StorageBlob extends DefaultStreamBlob implements SQLBlob {

    private static final long serialVersionUID = 1L;

    protected final Binary binary;

    protected final long length;

    public StorageBlob(Binary binary, String filename, String mimeType,
            String encoding, String digest, long length) {
        this.binary = binary;
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

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public Blob persist() {
        return this;
    }

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
}
