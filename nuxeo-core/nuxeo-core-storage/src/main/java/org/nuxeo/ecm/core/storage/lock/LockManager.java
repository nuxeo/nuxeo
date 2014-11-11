/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.lock;

import org.nuxeo.ecm.core.api.Lock;

/**
 * Manager of locks for a repository.
 * <p>
 * The method {@link #close} must be called when done with the lock manager.
 *
 * @since 5.9.6
 */
public interface LockManager {

    /**
     * Gets the lock on a document.
     * <p>
     * If the document does not exist, {@code null} is returned.
     *
     * @param id the document id
     * @return the existing lock, or {@code null} when there is no lock
     */
    Lock getLock(String id);

    /**
     * Sets a lock on a document.
     * <p>
     * If the document is already locked, returns its existing lock status
     * (there is no re-locking, {@link #removeLock} must be called first).
     *
     * @param id the document id
     * @param lock the lock to set
     * @return {@code null} if locking succeeded, or the existing lock if
     *         locking failed
     */
    Lock setLock(String id, Lock lock);

    /**
     * Removes the lock from a document.
     * <p>
     * The previous lock is returned.
     * <p>
     * If {@code owner} is {@code null} then the lock is unconditionally
     * removed.
     * <p>
     * If {@code owner} is not {@code null}, it must match the existing lock
     * owner for the lock to be removed. If it doesn't match, the returned lock
     * will return {@code true} for {@link Lock#getFailed}.
     *
     * @param id the document id
     * @param the owner to check, or {@code null} for no check
     * @param force {@code true} to just do the remove and not return the
     *            previous lock
     * @return the previous lock (may be {@code null}), with a failed flag if
     *         locking failed
     */
    Lock removeLock(String id, String owner);

    /**
     * Checks if a given lock can be removed by the given owner.
     *
     * @param lock the lock
     * @param owner the owner (may be {@code null})
     * @return {@code true} if the lock can be removed
     */
    boolean canLockBeRemoved(Lock lock, String owner);

    /**
     * Closes the lock manager and releases resources.
     */
    void close();

    /**
     * Clears any cache held by the lock manager.
     */
    void clearCaches();

}
