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

}
