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

package org.nuxeo.ecm.core.storage.sql;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * The transactional session is an {@link XAResource} for this session.
 *
 * @author Florent Guillaume
 */
public class TransactionalSession implements XAResource {

    private final Mapper mapper;

    private final PersistenceContext context;

    private boolean inTransaction;

    TransactionalSession(Mapper mapper, PersistenceContext context) {
        this.mapper = mapper;
        this.context = context;
    }

    public boolean isInTransaction() {
        return inTransaction;
    }

    /*
     * ----- javax.transaction.xa.XAResource -----
     */

    public boolean isSameRM(XAResource xaresource) {
        return xaresource == this;
    }

    public void start(Xid xid, int flags) throws XAException {
        if (flags == TMNOFLAGS) {
            context.processInvalidations();
        }
        mapper.start(xid, flags);
        inTransaction = true;
    }

    public void end(Xid xid, int flags) throws XAException {
        mapper.end(xid, flags);
    }

    public int prepare(Xid xid) throws XAException {
        return mapper.prepare(xid);
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            mapper.commit(xid, onePhase);
        } finally {
            inTransaction = false;
            context.notifyInvalidations();
        }
    }

    public void rollback(Xid xid) throws XAException {
        try {
            mapper.rollback(xid);
        } finally {
            inTransaction = false;
            context.notifyInvalidations();
        }
    }

    public void forget(Xid xid) throws XAException {
        mapper.forget(xid);
    }

    public Xid[] recover(int flag) throws XAException {
        return mapper.recover(flag);
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        return mapper.setTransactionTimeout(seconds);
    }

    public int getTransactionTimeout() throws XAException {
        return mapper.getTransactionTimeout();
    }

}
