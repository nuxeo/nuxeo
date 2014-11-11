/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import static javax.transaction.xa.XAException.XAER_INVAL;
import static javax.transaction.xa.XAException.XAER_PROTO;
import static javax.transaction.xa.XAException.XAER_RMERR;

import java.sql.Connection;
import java.sql.SQLException;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Adapter for a simple JDBC Connection that gives it the XAResource interface,
 * without actually implementing XA (prepare does nothing).
 *
 * @since 5.7
 */
public class XAResourceConnectionAdapter implements XAResource {

    private static final Log log = LogFactory.getLog(XAResourceConnectionAdapter.class);

    protected final Connection connection;

    protected Xid xid;

    public XAResourceConnectionAdapter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void start(Xid xid, int flag) throws XAException {
        if (flag == TMNOFLAGS) {
            if (this.xid != null) {
                throw newXAException(XAER_PROTO, "Already started");
            }
            this.xid = xid;
            try {
                connection.setAutoCommit(false);
            } catch (SQLException e) {
                throw newXAException(XAER_RMERR, "Cannot set autoCommit=false");
            }
        } else {
            // cannot support resume
            throw newXAException(XAER_INVAL, "Invalid flag: " + flag);
        }
    }

    @Override
    public void end(Xid xid, int flag) throws XAException {
        if (xid != this.xid) {
            throw newXAException(XAER_INVAL, "Invalid Xid");
        }
        if (flag != TMSUCCESS && flag != TMFAIL) {
            throw newXAException(XAER_INVAL, "Invalid flag: " + flag);
        }
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        return XA_OK;
    }

    @Override
    public void commit(Xid xid, boolean flag) throws XAException {
        if (this.xid == null || !this.xid.equals(xid)) {
            throw newXAException(XAER_INVAL, "Invalid Xid");
        }
        this.xid = null;
        try {
            connection.commit();
        } catch (SQLException e) {
            throw newXAException(XAER_RMERR, "Cannot commit", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                log.error("Cannot set autoCommit=true", e);
            }
        }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        if (this.xid == null || !this.xid.equals(xid)) {
            throw newXAException(XAER_INVAL, "Invalid Xid");
        }
        this.xid = null;
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw newXAException(XAER_RMERR, "Cannot rollback", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                log.error("Cannot set autoCommit=true", e);
            }
        }
    }

    @Override
    public void forget(Xid xid) throws XAException {
        throw newXAException(XAER_PROTO, "Unsupported method");
    }

    @Override
    public Xid[] recover(int n) throws XAException {
        return new Xid[0];
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    @Override
    public boolean setTransactionTimeout(int txTimeout) throws XAException {
        return false;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        return this == xares;
    }

    protected static XAException newXAException(int errorCode, String message,
            Exception cause) {
        return (XAException) newXAException(errorCode, message).initCause(cause);
    }

    protected static XAException newXAException(int errorCode, String message) {
        XAException e = new XAException(message);
        e.errorCode = errorCode;
        return e;
    }

}
