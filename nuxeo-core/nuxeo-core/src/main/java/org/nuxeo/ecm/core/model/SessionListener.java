/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
