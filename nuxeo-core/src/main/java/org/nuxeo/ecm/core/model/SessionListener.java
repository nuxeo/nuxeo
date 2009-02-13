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

/**
 * Listener to be notified about events in session life cycle.
 *
 * @author bstefanescu
 */
public interface SessionListener {

    /**
     * The session is about to be closed.
     *
     * @param session the session
     */
    void aboutToCloseSession(Session session);

    /**
     * The session was closed.
     *
     * @param session the session
     */
    void sessionClosed(Session session);

    /**
     * The session was started.
     *
     * @param session the session
     */
    void sessionStarted(Session session);

}
