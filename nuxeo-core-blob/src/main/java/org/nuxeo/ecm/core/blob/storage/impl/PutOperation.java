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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.nuxeo.ecm.core.blob.storage.BlobStorageException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PutOperation implements BlobOperation {

    public final static String DIGEST_ALGO = "sha-256";


    protected DefaultBlobStorage storage;
    protected InputStream in;
    protected DefaultBlobResource result;
    protected boolean updated = false;

    public PutOperation(DefaultBlobStorage storage, InputStream in) {
        this.storage = storage;
        this.in = in;
    }


    public void execute() throws BlobStorageException {
        File tmp = null;
        try {
            tmp = File.createTempFile("nx_blobStore_", ".tmp");
            String hash = storeWithDigest(in, tmp);
            File file = storage.getBlobDirectory(hash);
            file.mkdirs();
            file = new File(file, hash);
            long tm = 0;
            // ------> synchronized
            synchronized (storage) {
                if (!file.exists()) {
                    tmp.renameTo(file);
                    tm = file.lastModified();
                } else {
                    tm = System.currentTimeMillis();
                    file.setLastModified(tm); // used to correctly remove blobs
                    updated = true;
                }
            }
            // <-------
            result = new DefaultBlobResource(file, hash, tm);
        } catch (Exception e) {
            throw new BlobStorageException("Put blob failed", e);
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
        }
    }

    /**
     * @return the result.
     */
    public DefaultBlobResource getResult() {
        return result;
    }

    public void rollback() throws BlobStorageException {
        if (!updated && result != null) {
            result.file.delete();
        }
    }

    public void commit() throws BlobStorageException {
        // do nothing
    }


    protected String storeWithDigest(InputStream in, File tmp) throws IOException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(DIGEST_ALGO);
        } catch (NoSuchAlgorithmException e) {
            IOException ee = new IOException();
            ee.initCause(e);
            throw ee;
        }

        byte[] buf = storage.createBuffer(in);
        FileOutputStream out = new FileOutputStream(tmp);
        int read;

        try {
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                md.update(buf, 0, read);
            }
        } finally {
            out.close();
        }

        byte[] b = md.digest();
        return Hex.toHexString(b);
    }


}
