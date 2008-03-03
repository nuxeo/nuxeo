/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 * $Id$
 */

package org.nuxeo.ecm.core.model;

import org.nuxeo.ecm.core.api.DocumentException;

/**
 * An object that can be locked.
 * <p>
 * A lock is identified by a lock key.
 * The key is used to store information about the lock owner.
 *
 * @author bstefanescu
 *
 */
public interface Lockable {

    /**
     * Set a lock on the current document.
     * <p>
     * The lock key cannot be null
     *
     * @param key the lock key
     */
    void setLock(String key) throws DocumentException;

    /**
     * Tests if the current object is locked.
     *
     * @return true if locked false otherwise
     */
    boolean isLocked() throws DocumentException;

    /**
     * Gets the lock key if a lock exists on the current object.
     *
     * @return the lock key or null if no lock exists
     */
    String getLock() throws DocumentException;

    /**
     * Removes the lock on the object if any exists, otherwise do nothing.
     *
     * @return the key of the removed lock or null if the object was not locked
     */
    String unlock() throws DocumentException;

}
