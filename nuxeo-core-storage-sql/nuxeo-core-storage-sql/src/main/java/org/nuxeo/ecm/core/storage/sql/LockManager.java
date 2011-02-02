/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.io.Serializable;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.nuxeo.common.utils.XidImpl;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * Manager of locks that serializes access to them.
 * <p>
 * The public methods called by the session are {@link #setLock},
 * {@link #removeLock} and {@link #getLock}. Method {@link #shutdown} must be
 * called when done with the lock manager.
 * <p>
 * In cluster mode, changes are executed in a begin/commit so that tests/updates
 * can be atomic.
 * <p>
 * Transaction management can be done by hand because we're dealing with a
 * low-level {@link SessionImpl} and not something wrapped by a JCA pool.
 */
public class LockManager {

    /**
     * The session to use. In this session we only ever touch the lock table, so
     * no need to deal with fulltext and complex saves, and we don't do
     * prefetch.
     */
    public final SessionImpl session;

    /**
     * If clustering is enabled then we have to wrap test/set and test/remove in
     * a transaction.
     */
    public final boolean clusteringEnabled;

    /**
     * Lock serializing access to the session.
     */
    protected final ReentrantLock sessionLock;

    /**
     * Creates a lock manager using the given session.
     * <p>
     * The session will from then on be only used and closed by the lock
     * manager.
     * <p>
     * {@link #shutdown} must be called when done with the lock manager.
     */
    public LockManager(SessionImpl session, boolean clusteringEnabled) {
        this.session = session;
        session.setSynchronousInvalidations();
        this.clusteringEnabled = clusteringEnabled;
        sessionLock = new ReentrantLock(true); // fair
    }

    /**
     * Shuts down the lock manager.
     */
    public void shutdown() throws StorageException {
        session.closeSession();
    }

    /**
     * Gets the lock on a document.
     */
    public Lock getLock(final Serializable id) throws StorageException {
        return call(false, new Callable<Lock>() {
            @Override
            public Lock call() throws Exception {
                Node node = session.getNodeById(id, false);
                return session.getLock(node);
            }
        });
    }

    /**
     * Locks a document.
     */
    public Lock setLock(final Serializable id, final Lock lock)
            throws StorageException {
        try {
            return setLockInternal(id, lock);
        } catch (StorageException e) {
            Throwable c = e.getCause();
            if (c != null && c instanceof SQLException) {
                boolean duplicateKey = isDuplicateKeyException((SQLException) c);
                if (duplicateKey) {
                    // cluster: got duplicate key (two simultaneous inserts)
                    // retry once, after the first insert is committed we'll get
                    // an invalidation and can just update
                    return setLockInternal(id, lock);
                }
            }
            throw e;
        }
    }

    protected boolean isDuplicateKeyException(SQLException e) {
        String sqlState = e.getSQLState();
        if ("23000".equals(sqlState)) {
            // MySQL: Duplicate entry ... for key ...
            // Oracle: unique constraint ... violated
            // SQL Server: Violation of PRIMARY KEY constraint
            return true;
        }
        if ("23001".equals(sqlState)) {
            // H2: Unique index or primary key violation
            return true;
        }
        if ("23505".equals(sqlState)) {
            // PostgreSQL: duplicate key value violates unique constraint
            return true;
        }
        return false;
    }

    protected Lock setLockInternal(final Serializable id, final Lock lock)
            throws StorageException {
        return call(true, new Callable<Lock>() {
            @Override
            public Lock call() throws Exception {
                Node node = session.getNodeById(id, false);
                Lock oldLock = session.setLock(node, lock);
                session.flushWithoutFulltext();
                return oldLock;
            }
        });
    }

    /**
     * Unlocks a document.
     */
    public Lock removeLock(final Serializable id, final String owner)
            throws StorageException {
        return call(true, new Callable<Lock>() {
            @Override
            public Lock call() throws Exception {
                Node node = session.getNodeById(id, false);
                Lock oldLock = session.removeLock(node, owner);
                session.flushWithoutFulltext();
                return oldLock;
            }
        });
    }

    /**
     * Calls the callable, under a begin/commit if in cluster mode.
     */
    protected Lock call(boolean hasWrites, Callable<Lock> callable)
            throws StorageException {
        sessionLock.lock();
        try {
            Xid xid = null;
            boolean txStarted = false;
            boolean txSuccess = false;
            try {
                if (hasWrites && clusteringEnabled) {
                    xid = new XidImpl("nuxeolockmanager"
                            + System.currentTimeMillis());
                    try {
                        session.start(xid, XAResource.TMNOFLAGS);
                    } catch (XAException e) {
                        throw new StorageException(e);
                    }
                    txStarted = true;
                } else {
                    // must still process received invalidations, as locks may
                    // be written from a normal session on document creation
                    session.processReceivedInvalidations();
                }

                // actual call
                Lock result;
                try {
                    result = callable.call();
                } catch (StorageException e) {
                    throw e;
                } catch (Exception e) {
                    throw new StorageException(e);
                }

                txSuccess = true;
                return result;
            } finally {
                if (txStarted) {
                    try {
                        if (txSuccess) {
                            session.end(xid, XAResource.TMSUCCESS);
                            session.commit(xid, true);
                        } else {
                            session.end(xid, XAResource.TMFAIL);
                            session.rollback(xid);
                        }
                    } catch (XAException e) {
                        throw new StorageException(e);
                    }
                }
            }
        } finally {
            sessionLock.unlock();
        }
    }

}
