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

package org.nuxeo.ecm.core.storage.sql.management;

import org.nuxeo.ecm.core.storage.sql.BinaryManagerStatus;

/**
 * @author Florent Guillaume
 */
public interface RepositoryStatusMBean {

    /**
     * Lists the opened sessions.
     */
    String listActiveSessions();

    /**
     * Lists the remote opened sessions
     */
    String listRemoteSessions();

    /**
     * Return the opened sessions count
     */
    int getActiveSessionsCount();

    /**
     * Clears the caches.
     */
    String clearCaches();

    /**
     * GC the unused binaries.
     *
     * @param delete if {@code false} don't actually delete the GCed binaries
     *            (but still return statistics about them), if {@code true}
     *            delete them
     * @return a status about the number of GCed binaries
     */
    BinaryManagerStatus gcBinaries(boolean delete);

    /**
     * Is a GC of the binaries in progress?
     * <p>
     * It's only useful to call this from a separate thread from the one that
     * called {@link #gcBinaries}.
     *
     * @return {@code true} if a GC of the binaries is in progress
     */
    boolean isBinariesGCInProgress();

}
