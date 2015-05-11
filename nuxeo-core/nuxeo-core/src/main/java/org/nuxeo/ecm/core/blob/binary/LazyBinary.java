/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Lazy Binary that fetches its remote stream on first access.
 */
public class LazyBinary extends Binary {

    private static final long serialVersionUID = 1L;

    protected boolean hasLength;

    // transient to be Serializable
    protected transient CachingBinaryManager cbm;

    public LazyBinary(String digest, String repoName, CachingBinaryManager cbm) {
        super(digest, repoName);
        this.cbm = cbm;
    }

    // because the class is Serializable, re-acquire the CachingBinaryManager
    protected CachingBinaryManager getCachingBinaryManager() {
        if (cbm == null) {
            if (blobProviderId == null) {
                throw new UnsupportedOperationException("Cannot find binary manager, no blob provider id");
            }
            BlobManager bm = Framework.getService(BlobManager.class);
            BinaryBlobProvider bbp = (BinaryBlobProvider) bm.getBlobProvider(blobProviderId);
            cbm = (CachingBinaryManager) bbp.getBinaryManager();
        }
        return cbm;
    }

    @Override
    public InputStream getStream() throws IOException {
        File file = getFile();
        return file == null ? null : new FileInputStream(file);
    }

    @Override
    public File getFile() {
        if (file == null) {
            try {
                file = getCachingBinaryManager().getFile(digest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (file != null) {
                length = file.length();
                hasLength = true;
            }
        }
        return file;
    }

    @Override
    public long getLength() {
        if (!hasLength) {
            Long len;
            try {
                len = getCachingBinaryManager().getLength(digest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            length = len == null ? 0 : len.longValue();
            hasLength = true;
        }
        return length;
    }

}
