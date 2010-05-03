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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.resource.ResourceException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.storage.BlobResource;
import org.nuxeo.ecm.core.blob.storage.BlobStorageException;
import org.nuxeo.ecm.core.blob.storage.BlobStorageSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class XASession implements XAResource, BlobStorageSession {

    private static final Log log = LogFactory.getLog(XASession.class);

    protected static final int DEAULT_TIMEOUT = 10;

    protected int timeout = DEAULT_TIMEOUT;
    protected DefaultBlobStorage storage;

    protected ConcurrentMap<Xid,TransactionContext> transactions;

    protected TransactionContext transaction;


    /**
     *
     */
    public XASession(DefaultBlobStorage storage) {
        this.storage = storage;
        this.transactions = new ConcurrentHashMap<Xid, TransactionContext>();
    }

    /**
     * @return the storage.
     */
    public DefaultBlobStorage getStorage() {
        return storage;
    }


    public BlobResource get(String hash) throws BlobStorageException {
        return storage.get(hash);
    }

    public BlobResource put(InputStream in) throws BlobStorageException {
        if (transaction != null) {
            return transaction.put(in);
        }
        PutOperation op = new PutOperation(storage, in);
        op.execute();
        return op.getResult();
    }

    public void remove(String hash) throws BlobStorageException {
        if (transaction != null) {
            transaction.remove(hash);
        } else {
            new RemoveOperation(storage, hash).execute();
        }
    }



    public int getTransactionTimeout() throws XAException {
        return timeout;
    }

    public boolean setTransactionTimeout(int arg0) throws XAException {
        timeout = arg0 == 0 ? DEAULT_TIMEOUT: arg0;
        return true;
    }

    public boolean isSameRM(XAResource xares) throws XAException {
        return xares instanceof XASession;
    }


    protected boolean isAssociated() {
        return transaction != null;
    }

    protected void associate(TransactionContext transaction) {
        this.transaction = transaction;
    }

    protected TransactionContext createTransaction(Xid xid) {
        TransactionContext tr = new TransactionContext(storage, xid);
        transactions.put(xid, tr);
        return tr;
    }

    public void start(Xid xid, int flags) throws XAException {
        if (isAssociated()) {
            log.error("Resource already associated with a transaction.");
            throw new XAException(XAException.XAER_PROTO);
        }
        TransactionContext tx = (TransactionContext) transactions.get(xid);
        if (flags == TMNOFLAGS) {
            if (tx != null) {
                throw new XAException(XAException.XAER_DUPID);
            }
            tx = createTransaction(xid);
        } else if (flags == TMJOIN) {
            if (tx == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
        } else if (flags == TMRESUME) {
            if (tx == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
            if (!tx.isSuspended()) {
                log.error("Unable to resume: transaction not suspended.");
                throw new XAException(XAException.XAER_PROTO);
            }
            tx.setSuspended(false);
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }

        associate(tx);
    }

    public void end(Xid xid, int flags) throws XAException {
        TransactionContext tx = (TransactionContext) transactions.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        if (flags == TMSUSPEND) {
            if (!isAssociated()) {
                log.error("Resource not associated with a transaction.");
                throw new XAException(XAException.XAER_PROTO);
            }
            associate(null);
            tx.setSuspended(true);
        } else if (flags == TMFAIL || flags == TMSUCCESS) {
            if (!tx.isSuspended()) {
                if (!isAssociated()) {
                    log.error("Resource not associated with a transaction.");
                    throw new XAException(XAException.XAER_PROTO);
                }
                associate(null);
            } else {
                tx.setSuspended(false);
            }
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        TransactionContext tx = (TransactionContext) transactions.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        try {
            if (onePhase) {
                tx.prepare();
            }
            tx.commit();
        } catch (ResourceException e) {
            XAException ee = new XAException(XAException.XAER_RMERR);
            ee.initCause(e);
            throw ee;
        }

        transactions.remove(xid);
    }

    public int prepare(Xid xid) throws XAException {
        TransactionContext tx = (TransactionContext) transactions.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        try {
            tx.prepare();
        } catch (ResourceException e) {
            XAException ee = new XAException(XAException.XAER_RMERR);
            ee.initCause(e);
            throw ee;
        }
        return XA_OK;
    }

    public void rollback(Xid xid) throws XAException {
        TransactionContext tx = (TransactionContext) transactions.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        try {
            tx.rollback();
        } catch (ResourceException e) {
            XAException ee = new XAException(XAException.XAER_RMERR);
            ee.initCause(e);
            throw ee;
        }
        transactions.remove(xid);
    }

    public void forget(Xid xid) throws XAException {
        // no recovery support
    }

    public Xid[] recover(int flag) throws XAException {
        // no recovery support
        return new Xid[0];
    }

}
