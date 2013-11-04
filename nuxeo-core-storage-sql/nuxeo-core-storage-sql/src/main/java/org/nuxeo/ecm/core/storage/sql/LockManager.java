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
package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.storage.ConcurrentUpdateStorageException;
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
 * low-level {@link Mapper} and not something wrapped by a JCA pool.
 */
public class LockManager {

    private static final Log log = LogFactory.getLog(LockManager.class);

    /**
     * The mapper to use. In this mapper we only ever touch the lock table, so
     * no need to deal with fulltext and complex saves, and we don't do
     * prefetch.
     */
    protected final Mapper mapper;

    /**
     * If clustering is enabled then we have to wrap test/set and test/remove in
     * a transaction.
     */
    protected final boolean clusteringEnabled;

    /**
     * Lock serializing access to the mapper.
     */
    protected final ReentrantLock serializationLock;

    protected static final Lock NULL_LOCK = new Lock(null, null);

    protected final boolean caching;

    /**
     * A cache of locks, used only in non-cluster mode, when this lock manager
     * is the only one dealing with locks.
     * <p>
     * Used under {@link #serializationLock}.
     */
    protected final LRUCache<Serializable, Lock> lockCache;

    protected static final int CACHE_SIZE = 100;

    protected static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;

        private final int max;

        public LRUCache(int max) {
            super(max, 1.0f, true);
            this.max = max;
        }

        @Override
        protected boolean removeEldestEntry(Entry<K, V> eldest) {
            return size() > max;
        }
    }

    /**
     * Creates a lock manager using the given mapper.
     * <p>
     * The mapper will from then on be only used and closed by the lock manager.
     * <p>
     * {@link #shutdown} must be called when done with the lock manager.
     */
    public LockManager(Mapper mapper, boolean clusteringEnabled)
            throws StorageException {
        this.mapper = mapper;
        this.clusteringEnabled = clusteringEnabled;
        serializationLock = new ReentrantLock(true); // fair
        caching = !clusteringEnabled;
        lockCache = caching ? new LRUCache<Serializable, Lock>(CACHE_SIZE)
                : null;
    }

    /**
     * Shuts down the lock manager.
     */
    public void shutdown() throws StorageException {
        serializationLock.lock();
        try {
            mapper.close();
        } finally {
            serializationLock.unlock();
        }
    }

    /**
     * Gets the lock on a document.
     */
    public Lock getLock(final Serializable id) throws StorageException {
        serializationLock.lock();
        try {
            Lock lock;
            if (caching && (lock = lockCache.get(id)) != null) {
                return lock == NULL_LOCK ? null : lock;
            }
            // no transaction needed, single operation
            lock = mapper.getLock(id);
            if (caching) {
                lockCache.put(id, lock == null ? NULL_LOCK : lock);
            }
            return lock;
        } finally {
            serializationLock.unlock();
        }
    }

    /**
     * Locks a document.
     */
    public Lock setLock(Serializable id, Lock lock) throws StorageException {
        int RETRIES = 10;
        long sleepDelay = 1; // 1 ms
        long INCREMENT = 50; // additional 50 ms each time
        for (int i = 0; i < RETRIES; i++) {
            if (i > 0) {
                log.debug("Retrying lock on " + id + ": try " + (i + 1));
            }
            try {
                return setLockInternal(id, lock);
            } catch (StorageException e) {
                if (shouldRetry(e)) {
                    // cluster: two simultaneous inserts
                    // retry
                    try {
                        Thread.sleep(sleepDelay);
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                    sleepDelay += INCREMENT;
                    continue;
                }
                throw e;
            }
        }
        throw new StorageException("Failed to lock " + id
                + ", too much concurrency (tried " + RETRIES + " times)");
    }

    /**
     * Does the exception mean that we should retry the transaction?
     */
    protected boolean shouldRetry(StorageException e) {
        if (e instanceof ConcurrentUpdateStorageException) {
            return true;
        }
        Throwable t = e.getCause();
        if (t instanceof BatchUpdateException) {
            t = t.getCause();
        }
        return t instanceof SQLException && shouldRetry((SQLException) t);
    }

    protected boolean shouldRetry(SQLException e) {
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
        if ("S0003".equals(sqlState) || "S0005".equals(sqlState)) {
            // SQL Server: Snapshot isolation transaction aborted due to update
            // conflict
            return true;
        }
        return false;
    }

    protected Lock setLockInternal(final Serializable id, final Lock lock)
            throws StorageException {
        serializationLock.lock();
        try {
            Lock oldLock;
            if (caching && (oldLock = lockCache.get(id)) != null
                    && oldLock != NULL_LOCK) {
                return oldLock;
            }
            oldLock = mapper.setLock(id, lock);
            if (caching && oldLock == null) {
                lockCache.put(id, lock == null ? NULL_LOCK : lock);
            }
            return oldLock;
        } finally {
            serializationLock.unlock();
        }
    }

    /**
     * Unlocks a document.
     */
    public Lock removeLock(final Serializable id, final String owner)
            throws StorageException {
        serializationLock.lock();
        try {
            Lock oldLock = null;
            if (caching && (oldLock = lockCache.get(id)) == NULL_LOCK) {
                return null;
            }
            if (oldLock != null && !canLockBeRemoved(oldLock, owner)) {
                // existing mismatched lock, flag failure
                oldLock = new Lock(oldLock, true);
            } else {
                if (oldLock == null) {
                    oldLock = mapper.removeLock(id, owner, false);
                } else {
                    // we know the previous lock, we can force
                    // no transaction needed, single operation
                    mapper.removeLock(id, owner, true);
                }
            }
            if (caching) {
                if (oldLock != null && oldLock.getFailed()) {
                    // failed, but we now know the existing lock
                    lockCache.put(id, new Lock(oldLock, false));
                } else {
                    lockCache.put(id, NULL_LOCK);
                }
            }
            return oldLock;
        } finally {
            serializationLock.unlock();
        }
    }


    public void clearCaches() {
        serializationLock.lock();
        try {
            if (caching) {
                lockCache.clear();
            }
        } finally {
            serializationLock.unlock();
        }
    }

    @Override
    public String toString() {
        return "LockManager [mapper=" + mapper + "]";
    }

    /**
     * Checks if a given lock can be removed by the given owner.
     *
     * @param lock the lock
     * @param owner the owner (may be {@code null})
     * @return {@code true} if the lock can be removed
     */
    public static boolean canLockBeRemoved(Lock lock, String owner) {
        return owner == null || owner.equals(lock.getOwner());
    }

}
