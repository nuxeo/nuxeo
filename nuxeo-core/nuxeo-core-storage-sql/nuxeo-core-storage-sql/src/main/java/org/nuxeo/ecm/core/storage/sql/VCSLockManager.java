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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.storage.ConcurrentUpdateStorageException;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.lock.AbstractLockManager;
import org.nuxeo.ecm.core.storage.lock.LockException;
import org.nuxeo.ecm.core.storage.sql.RepositoryBackend.MapperKind;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.runtime.api.Framework;

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
public class VCSLockManager extends AbstractLockManager {

    private static final Log log = LogFactory.getLog(VCSLockManager.class);

    public static final int LOCK_RETRIES = 10;

    public static final long LOCK_SLEEP_DELAY = 1; // 1 ms

    public static final long LOCK_SLEEP_INCREMENT = 50; // add 50 ms each time

    protected final RepositoryImpl repository;

    /**
     * The mapper to use. In this mapper we only ever touch the lock table, so
     * no need to deal with fulltext and complex saves, and we don't do
     * prefetch.
     */
    protected Mapper mapper;

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
     * Creates a lock manager for the given repository.
     * <p>
     * The mapper will from then on be only used and closed by the lock manager.
     * <p>
     * {@link #close} must be called when done with the lock manager.
     */
    public VCSLockManager(String repositoryName) throws StorageException {
        SQLRepositoryService repositoryService = Framework.getService(SQLRepositoryService.class);
        repository = repositoryService.getRepositoryImpl(repositoryName);
        clusteringEnabled = repository.getRepositoryDescriptor().getClusteringEnabled();
        serializationLock = new ReentrantLock();
        caching = !clusteringEnabled;
        lockCache = caching ? new LRUCache<Serializable, Lock>(CACHE_SIZE)
                : null;
    }

    /**
     * Delay mapper acquisition until the repository has been fully initialized.
     */
    protected Mapper getMapper() throws StorageException {
        if (mapper == null) {
            mapper = repository.getBackend().newMapper(repository.getModel(),
                    null, MapperKind.LOCK_MANAGER);
        }
        return mapper;
    }

    protected Serializable idFromString(String id) {
        return repository.getModel().idFromString(id);
    }

    @Override
    public void close() {
        serializationLock.lock();
        try {
            try {
                getMapper().close();
            } catch (StorageException e) {
                throw new NuxeoException(e);
            }
        } finally {
            serializationLock.unlock();
        }
    }

    @Override
    public Lock getLock(final String id) {
        serializationLock.lock();
        try {
            Lock lock;
            if (caching && (lock = lockCache.get(id)) != null) {
                return lock == NULL_LOCK ? null : lock;
            }
            // no transaction needed, single operation
            try {
                lock = getMapper().getLock(idFromString(id));
            } catch (StorageException e) {
                throw new LockException(e);
            }
            if (caching) {
                lockCache.put(id, lock == null ? NULL_LOCK : lock);
            }
            return lock;
        } finally {
            serializationLock.unlock();
        }
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        // We don't call addSuppressed() on an existing exception
        // because constructing it beforehand when it most likely
        // won't be needed is expensive.
        List<Throwable> suppressed = new ArrayList<>(0);
        long sleepDelay = LOCK_SLEEP_DELAY;
        for (int i = 0; i < LOCK_RETRIES; i++) {
            if (i > 0) {
                log.debug("Retrying lock on " + id + ": try " + (i + 1));
            }
            try {
                return setLockInternal(id, lock);
            } catch (StorageException e) {
                suppressed.add(e);
                if (shouldRetry(e)) {
                    // cluster: two simultaneous inserts
                    // retry
                    try {
                        Thread.sleep(sleepDelay);
                    } catch (InterruptedException ie) {
                        // restore interrupted status
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                    sleepDelay += LOCK_SLEEP_INCREMENT;
                    continue;
                }
                // not something to retry
                LockException exception = new LockException(e);
                for (Throwable t : suppressed) {
                    exception.addSuppressed(t);
                }
                throw exception;
            }
        }
        LockException exception = new LockException("Failed to lock " + id
                + ", too much concurrency (tried " + LOCK_RETRIES + " times)");
        for (Throwable t : suppressed) {
            exception.addSuppressed(t);
        }
        throw exception;
    }

    /**
     * Does the exception mean that we should retry the transaction?
     */
    protected boolean shouldRetry(StorageException e) {
        if (e instanceof ConcurrentUpdateStorageException) {
            return true;
        }
        Throwable t = e.getCause();
        if (t instanceof BatchUpdateException && t.getCause() != null) {
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

    protected Lock setLockInternal(String id, Lock lock)
            throws StorageException {
        serializationLock.lock();
        try {
            Lock oldLock;
            if (caching && (oldLock = lockCache.get(id)) != null
                    && oldLock != NULL_LOCK) {
                return oldLock;
            }
            oldLock = getMapper().setLock(idFromString(id), lock);
            if (caching && oldLock == null) {
                lockCache.put(id, lock == null ? NULL_LOCK : lock);
            }
            return oldLock;
        } finally {
            serializationLock.unlock();
        }
    }

    @Override
    public Lock removeLock(final String id, final String owner) {
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
                try {
                    if (oldLock == null) {
                        oldLock = getMapper().removeLock(idFromString(id),
                                owner, false);
                    } else {
                        // we know the previous lock, we can force
                        // no transaction needed, single operation
                        getMapper().removeLock(idFromString(id), owner, true);
                    }
                } catch (StorageException e) {
                    throw new LockException(e);
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

    @Override
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
        return getClass().getSimpleName() + '(' + repository.getName() + ')';
    }

}
