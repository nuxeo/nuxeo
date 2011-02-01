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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.nuxeo.common.utils.XidImpl;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * Manager of locks that does its work in a single thread where all operations
 * are serialized
 * <p>
 * The public methods called by the session are {@link #setLock},
 * {@link #removeLock} and {@link #getLock}. Method {@link #shutdown} must be
 * called when done with the lock manager.
 * <p>
 * In cluster mode, changes are executed in their own transaction so that
 * test/update can be atomic.
 * <p>
 * Transaction management is done by hand because we're dealing with a low-level
 * {@link SessionImpl} and not something wrapped by a JCA pool.
 */
public class LockManager extends Thread {

    protected static enum Method {
        GETLOCK, SETLOCK, REMOVELOCK
    }

    protected static final class MethodCall {
        public final Method method;

        public final Object[] args;

        public MethodCall(Method method, Object... args) {
            this.method = method;
            this.args = args;
        }
    }

    protected static final class MethodResult {
        public final Object result;

        public MethodResult(Object result) {
            this.result = result;
        }
    }

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

    protected final BlockingQueue<MethodCall> methodCalls;

    protected final BlockingQueue<MethodResult> methodResults;

    /**
     * Creates a lock manager using the given session.
     * <p>
     * The session will from then on be only used and closed by the lock
     * manager.
     * <p>
     * {@link #shutdown} must be called when done with the lock manager.
     */
    public LockManager(SessionImpl session, boolean clusteringEnabled) {
        super("Nuxeo LockManager (" + session.getRepositoryName() + ')');
        this.session = session;
        this.clusteringEnabled = clusteringEnabled;
        this.methodCalls = new LinkedBlockingQueue<MethodCall>(1);
        this.methodResults = new LinkedBlockingQueue<MethodResult>(1);
        start();
    }

    /**
     * Shuts down the lock manager.
     */
    public void shutdown() throws StorageException {
        try {
            interrupt();
            try {
                join();
            } catch (InterruptedException e) {
                // ignored
            }
        } finally {
            session.closeSession();
        }
    }

    /**
     * Gets the lock on a document.
     */
    public Lock getLock(Serializable id) throws StorageException {
        return (Lock) call(Method.GETLOCK, id);
    }

    /**
     * Locks a document.
     */
    public Lock setLock(Serializable id, Lock lock) throws StorageException {
        return (Lock) call(Method.SETLOCK, id, lock);
    }

    /**
     * Unlocks a document.
     */
    public Lock removeLock(Serializable id, String owner)
            throws StorageException {
        return (Lock) call(Method.REMOVELOCK, id, owner);
    }

    /**
     * Passes a call from the local thread to the lock manager thread.
     * <p>
     * Exceptions in the lock manager thread are propagated to the local thread.
     */
    protected Object call(Method method, Object... args)
            throws StorageException {
        Object result;
        try {
            methodCalls.put(new MethodCall(method, args));
            result = methodResults.take().result;
        } catch (InterruptedException e) {
            throw new StorageException(e);
        }
        if (result instanceof StorageException) {
            throw (StorageException) result;
        }
        return result;
    }

    @Override
    public void run() {
        try {
            while (true) {
                MethodCall call = methodCalls.take();
                Object res;
                try {
                    res = callWithTX(call.method, call.args);
                } catch (StorageException e) {
                    res = e;
                }
                methodResults.put(new MethodResult(res));
            }
        } catch (InterruptedException e) {
            // end
        }
    }

    // called in the LockManager thread
    protected Object callWithTX(Method method, Object[] args)
            throws StorageException {
        boolean tx = method != Method.GETLOCK && clusteringEnabled;
        Xid xid = null;
        boolean started = false;
        boolean success = false;
        try {
            if (tx) {
                xid = new XidImpl("nuxeolockmanagertx"
                        + System.currentTimeMillis());
                try {
                    session.start(xid, XAResource.TMNOFLAGS);
                } catch (XAException e) {
                    throw new StorageException(e);
                }
                started = true;
            } else {
                // must still process received invalidations, as locks may be
                // written from a normal session on document creation
                session.processReceivedInvalidations();
            }

            // actual work
            Object res = callInternal(method, args);

            success = true;
            return res;
        } finally {
            if (started) {
                try {
                    if (success) {
                        session.end(xid, XAResource.TMSUCCESS);
                        session.commit(xid, true);
                    } else {
                        session.end(xid, XAResource.TMFAIL);
                        session.rollback(xid);
                    }
                } catch (XAException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // called in the LockManager thread
    protected Object callInternal(Method method, Object[] args)
            throws StorageException {
        Node node;
        Lock oldLock;
        switch (method) {
        case GETLOCK:
            node = session.getNodeById((Serializable) args[0], false);
            return session.getLock(node);
        case SETLOCK:
            // node may be null if doc save not yet committed
            node = session.getNodeById((Serializable) args[0], false);
            Lock lock = (Lock) args[1];
            oldLock = session.setLock(node, lock);
            session.flushWithoutFulltext();
            return oldLock;
        case REMOVELOCK:
            // node may be null if doc save not yet committed
            node = session.getNodeById((Serializable) args[0], false);
            String owner = (String) args[1];
            oldLock = session.removeLock(node, owner);
            session.flushWithoutFulltext();
            return oldLock;
        }
        throw new AssertionError();
    }

}
