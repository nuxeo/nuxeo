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

package org.nuxeo.ecm.core.storage.sql.ra;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * A wrapping of a {@link XAResource} (actually of our TransactionalSession)
 * that closes all connection handles at transaction end.
 *
 * @author Florent Guillaume
 */
public class ConnectionAwareXAResource implements XAResource {

    private final XAResource xaresource;

    private final ManagedConnectionImpl managedConnection;

    public ConnectionAwareXAResource(XAResource xaresource,
            ManagedConnectionImpl managedConnection) {
        this.xaresource = xaresource;
        this.managedConnection = managedConnection;
    }

    @Override
    public boolean isSameRM(XAResource other) throws XAException {
        if (!(other instanceof ConnectionAwareXAResource)) {
            return false;
        }
        return xaresource.isSameRM(((ConnectionAwareXAResource) other).xaresource);
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        xaresource.start(xid, flags);
    }

    // Arjuna, in its ReaperThread, and through
    // TransactionReaper#check -> AtomicAction#cancel ->
    // TwoPhaseCoordinator#cancel -> BasicAction#Abort -> BasicAction#doAbort ->
    // XAResourceRecord#topLevelAbort
    // is suspected of calling this in parallel in several threads, thus the
    // synchronized keyword
    @Override
    public synchronized void end(Xid xid, int flags) throws XAException {
        try {
            xaresource.end(xid, flags);
        } finally {
            managedConnection.closeConnections();
        }
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        return xaresource.prepare(xid);
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        xaresource.commit(xid, onePhase);
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        xaresource.rollback(xid);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        xaresource.forget(xid);
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        return xaresource.recover(flag);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return xaresource.setTransactionTimeout(seconds);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return xaresource.getTransactionTimeout();
    }

}
