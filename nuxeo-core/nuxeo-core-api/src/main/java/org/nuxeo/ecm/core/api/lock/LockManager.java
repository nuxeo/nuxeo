/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.lock;

import org.nuxeo.ecm.core.api.Lock;

/**
 * Manager of locks for a repository.
 * <p>
 * The method {@link #close} must be called when done with the lock manager.
 *
 * @since 6.0
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
     * If the document is already locked, returns its existing lock status (there is no re-locking, {@link #removeLock}
     * must be called first).
     *
     * @param id the document id
     * @param lock the lock to set
     * @return {@code null} if locking succeeded, or the existing lock if locking failed
     */
    Lock setLock(String id, Lock lock);

    /**
     * Removes the lock from a document.
     * <p>
     * The previous lock is returned.
     * <p>
     * If {@code owner} is {@code null} then the lock is unconditionally removed.
     * <p>
     * If {@code owner} is not {@code null}, it must match the existing lock owner for the lock to be removed. If it
     * doesn't match, the returned lock will return {@code true} for {@link Lock#getFailed}.
     *
     * @param id the document id
     * @param the owner to check, or {@code null} for no check
     * @param force {@code true} to just do the remove and not return the previous lock
     * @return the previous lock (may be {@code null}), with a failed flag if locking failed
     */
    Lock removeLock(String id, String owner);

    /**
     * Checks if a given lock can be removed by the given owner.
     *
     * @param oldOwner the existing lock's owner
     * @param owner the owner (may be {@code null})
     * @return {@code true} if the lock can be removed
     */
    static boolean canLockBeRemoved(String oldOwner, String owner) {
        return owner == null || owner.equals(oldOwner);
    }

    /**
     * Closes the lock manager and releases resources.
     */
    void closeLockManager();

    /**
     * Clears any cache held by the lock manager.
     */
    void clearLockManagerCaches();

}
