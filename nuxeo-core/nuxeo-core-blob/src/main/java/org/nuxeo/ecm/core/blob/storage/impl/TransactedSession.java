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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;

import org.nuxeo.ecm.core.blob.storage.BlobResource;
import org.nuxeo.ecm.core.blob.storage.BlobStorageException;
import org.nuxeo.ecm.core.blob.storage.BlobStorageSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TransactedSession implements BlobStorageSession, LocalTransaction {

    protected DefaultBlobStorage storage;
    protected List<BlobOperation> operations = null;

    /**
     *
     */
    public TransactedSession(DefaultBlobStorage storage) {
        this.storage = storage;
    }

    protected void execute(BlobOperation op) throws BlobStorageException {
        boolean ok = false;
        try {
            op.execute();
            ok = true;
        } finally {
            if (ok && operations != null) {
                operations.add(op);
            } else if (ok) {
                op.commit();
            }
        }
    }

    public BlobResource put(InputStream in) throws BlobStorageException {
        PutOperation op = new PutOperation(storage, in);
        execute(op);
        return op.getResult();
    }

    public void remove(String hash) throws BlobStorageException {
        RemoveOperation op = new RemoveOperation(storage, hash);
        execute(op);
    }

    public BlobResource get(String hash) throws BlobStorageException {
        return storage.get(hash);
    }

    public void begin() throws ResourceException {
        operations = new ArrayList<BlobOperation>();
    }

    public void rollback() throws ResourceException {
        if (operations != null) {
            for (BlobOperation op : operations) {
                try {
                    op.rollback();
                } catch (BlobStorageException e) {
                    e.printStackTrace(); //TODO
                }
            }
        }
        operations = null;
    }

    public void commit() throws ResourceException {
        if (operations != null) {
            for (BlobOperation op : operations) {
                try {
                    op.commit();
                } catch (BlobStorageException e) {
                    e.printStackTrace(); //TODO
                }
            }
        }
        operations = null;
    }

}
