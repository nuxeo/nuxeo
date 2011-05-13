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

    /**
     * Gets the binary GC for this repository.
     *
     * @return the binary garbage collector
     */
    BinaryGarbageCollector getBinaryGarbageCollector();

    /**
     * Marks the binaries actually in use with the GC so that they won't be
     * deleted.
     * <p>
     * The passed GC may or may not be the one returned by
     * {@link #getBinaryGarbageCollector} in case it's been determined that
     * another repository's GC is pointing to the same binary data.
     *
     * @param gc the binary garbage collector to use for this repository's
     *            binaries
     */
    void markReferencedBinaries(BinaryGarbageCollector gc);

}
