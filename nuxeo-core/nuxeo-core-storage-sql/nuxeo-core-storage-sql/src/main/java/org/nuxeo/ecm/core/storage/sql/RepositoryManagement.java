/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.util.Collection;

import org.nuxeo.ecm.core.storage.sql.net.MapperClientInfo;

/**
 * @author Florent Guillaume
 */
public interface RepositoryManagement {

    /**
     * Gets the repository name.
     */
    String getName();

    /**
     * Gets the number of active sessions.
     */
    int getActiveSessionsCount();

    /**
     * Clears all the caches.
     *
     * @return an indicative count of objects removed
     */
    int clearCaches();

    /**
     * Makes sure that the next transaction will process cluster invalidations.
     */
    void processClusterInvalidationsNext();

    /**
     * Is the server available remotely ?
     */
    boolean isServerActivated();

    /**
     * Which is the remote location ?
     * TODO this info would be better served by a provisioning service.
     * The remote location is dependent to the context of access.
     */
    String getServerURL();

    /**
     * Activates the VCS server used for remote connections.
     */
    void activateServer();

    /**
     * Deactivates the VCS server used for remote connections.
     */
    void deactivateServer();

    /**
     * Get info about current VCS server clients
     */
    Collection<MapperClientInfo> getClientInfos();

}
