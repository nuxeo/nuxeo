/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.lock.api;

import java.io.Serializable;
import java.net.URI;

/**
 * 
 * 
 * Coordinate two or more clients for operating on a resource.` Clients ask the
 * coordinator for a temporary lock on a resource. Coordinator let the winner
 * operating on the resource and block the others. If the winner do not return
 * to the coordinator in the delay he gave, one of the other takes the place and
 * the game is replayed.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public interface LockCoordinator {

    /**
     * Enter in the competition. Return immediatly if the resource is availble.
     * Otherwise wait for the expiration.
     * 
     * @param resource
     * @param timeout automatically unlock for that time
     * @throws AlreadyLockedException
     * @throws InterruptedException
     */
    void lock(URI self, URI resource, String comment, long timeout)
            throws AlreadyLockedException, InterruptedException;

    /**
     * Save addition information to the resource
     * 
     * @param self
     * @param resource
     * @param info
     * @throws InterruptedException
     */
    void saveInfo(URI self, URI resource, Serializable info)
            throws NotOwnerException, InterruptedException;

    /**
     * Unlock the resource. No further information will be available anymore
     * about this lock resource.
     * 
     * @param resource
     * @throws InterruptedException
     */
    void unlock(URI self, URI resource) throws NoSuchLockException,
            NotOwnerException, InterruptedException;

    /**
     * Return live lock information only.
     * 
     * @param resource
     * @return
     * @throws InterruptedException
     */
    LockInfo getInfo(URI resource) throws NoSuchLockException,
            InterruptedException;

}
