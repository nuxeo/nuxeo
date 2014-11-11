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

import org.nuxeo.ecm.core.blob.storage.BlobStorageException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RemoveOperation implements BlobOperation {

    protected DefaultBlobStorage storage;
    protected String hash;
    protected long lastModified;
    protected File toRemove;
    protected File trashFile;

    /**
     *
     */
    public RemoveOperation(DefaultBlobStorage storage, String hash) {
        this.storage = storage;
        this.hash = hash;
    }

    public void commit() throws BlobStorageException {
        if (trashFile != null) {
            trashFile.delete();
        }
    }

    public void execute() throws BlobStorageException {
        toRemove = storage.getBlobFile(hash);
        synchronized (storage) {
            if (toRemove.exists()) {
                try {
// TODO cannot remove file because iot may be in use by another thread.
                    lastModified = toRemove.lastModified();
                    trashFile = File.createTempFile("blobStorage_trash_", hash);
                    toRemove.renameTo(trashFile);
                } catch (Exception e) {
                    throw new BlobStorageException("Remove operation failed", e);
                }
            }
        }
    }

    public void rollback() throws BlobStorageException {
        if (trashFile != null) {
            synchronized (storage) {
                if (!toRemove.exists()) {
                    try {
                        trashFile.renameTo(toRemove);
                        toRemove.setLastModified(lastModified);
                    } catch (Exception e) {
                        throw new BlobStorageException("Remove operation failed", e);
                    }
                } else {
                    trashFile.delete();
                }
            }
        }
    }

}
