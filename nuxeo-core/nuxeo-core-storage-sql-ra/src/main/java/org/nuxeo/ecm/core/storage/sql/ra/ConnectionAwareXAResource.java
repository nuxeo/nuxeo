/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    private XAResource xaresource;

    private ManagedConnectionImpl managedConnection;

    public ConnectionAwareXAResource(XAResource xaresource,
            ManagedConnectionImpl managedConnection) {
        this.xaresource = xaresource;
        this.managedConnection = managedConnection;
    }

    public boolean isSameRM(XAResource other) throws XAException {
        if (!(other instanceof ConnectionAwareXAResource)) {
            return false;
        }
        return xaresource.isSameRM(((ConnectionAwareXAResource) other).xaresource);
    }

    public void start(Xid xid, int flags) throws XAException {
        xaresource.start(xid, flags);
    }

    public void end(Xid xid, int flags) throws XAException {
        try {
            xaresource.end(xid, flags);
        } finally {
            managedConnection.closeConnections();
        }
    }

    public int prepare(Xid xid) throws XAException {
        return xaresource.prepare(xid);
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        xaresource.commit(xid, onePhase);
    }

    public void rollback(Xid xid) throws XAException {
        xaresource.rollback(xid);
    }

    public void forget(Xid xid) throws XAException {
        xaresource.forget(xid);
    }

    public Xid[] recover(int flag) throws XAException {
        return xaresource.recover(flag);
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        return xaresource.setTransactionTimeout(seconds);
    }

    public int getTransactionTimeout() throws XAException {
        return xaresource.getTransactionTimeout();
    }

}
