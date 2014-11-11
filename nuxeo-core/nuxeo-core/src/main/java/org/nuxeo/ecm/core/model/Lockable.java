/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.model;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.Lock;

/**
 * An object that can be locked.
 * <p>
 * A lock holds a lock owner and lock creation time.
 */
public interface Lockable {

    /**
     * Sets a lock on the current document.
     *
     * @param lock the lock to set
     * @return the existing lock if locking couldn't be done
     */
    Lock setLock(Lock lock) throws DocumentException;

    /**
     * Removes a lock on the current document.
     *
     * @param the owner to check, or {@code null} for no check
     * @return {@code null} if there was no lock or if removal succeeded, or a
     *         lock if it blocks removal due to owner mismatch
     */
    Lock removeLock(String owner) throws DocumentException;

    /**
     * Gets the lock key if a lock exists on the current object.
     *
     * @return the lock or null if no lock exists
     */
    Lock getLock() throws DocumentException;

}
