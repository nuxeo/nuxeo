/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.blob.storage.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.blob.storage.BlobResource;
import org.nuxeo.ecm.core.blob.storage.BlobStorage;
import org.nuxeo.ecm.core.blob.storage.BlobStorageException;
import org.nuxeo.ecm.core.blob.storage.BlobStorageSession;

/**
 * A blob storage that use the file system to store blobs.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultBlobStorage implements BlobStorage {

    public final static int MIN_BUF_SIZE = 8*1024; // 8KB
    public final static int MAX_BUF_SIZE = 1024*1024; // 1MB

    protected File root;
    protected File data;

    public DefaultBlobStorage(File root) throws IOException {
        this.root = root.getCanonicalFile();
        this.data = new File(root, "data");
        this.data.mkdirs();
    }

    protected File getBlobDirectory(String hash) {
        if (hash.length() < 6) {
            throw new IllegalArgumentException("The file hash must have more than 6 characters.");
        }
        StringBuilder buf = new StringBuilder(16);
        buf.append(hash.substring(0,2));
        buf.append("/");
        buf.append(hash.substring(2,4));
        buf.append("/");
        buf.append(hash.substring(4,6));
        return new File(root, buf.toString());
    }

    protected File getBlobFile(String hash) {
        return new File(getBlobDirectory(hash), hash);
    }

    protected byte[] createBuffer(InputStream in) throws IOException {
        int size = in.available();
        if (size > 0) {
            if (size > MIN_BUF_SIZE) {
                size = size > MAX_BUF_SIZE ? MAX_BUF_SIZE : size;
            }
        } else {
            size = MAX_BUF_SIZE;
        }
        return new byte[size];
    }

    public BlobResource get(String hash) throws BlobStorageException {
        File file = getBlobFile(hash);
        if (file.exists()) {
            return new DefaultBlobResource(file, hash);
        }
        return null;
    }

    public BlobStorageSession getSession() {
        return new XASession(this);
    }

    public File getRoot() {
        return root;
    }
}

