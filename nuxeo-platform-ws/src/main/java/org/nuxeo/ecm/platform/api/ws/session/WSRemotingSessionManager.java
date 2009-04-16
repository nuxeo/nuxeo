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
 *     anguenot
 *
 * $Id: WSRemotingSessionManager.java 19483 2007-05-27 10:52:56Z sfermigier $
 */

package org.nuxeo.ecm.platform.api.ws.session;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Web service remoting session manager.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface WSRemotingSessionManager {

    /**
     * Creates a new remoting session.
     *
     * @param username the user name.
     * @param password the user password
     * @param repository the repository name
     * @param um the user manager
     * @param session the Nuxeo Core session
     *
     * @return an intialized <code>WSRemotingSession</code> instance.
     */
    WSRemotingSession createSession(String username, String password,
            String repository, UserManager um, CoreSession session);

    /**
     * Adds a new session.
     *
     * @param sid the nuxeo core session id.
     * @param session the web service remoting session.
     */
    void addSession(String sid, WSRemotingSession session);

    /**
     * Deletes a session if it exists.
     *
     * @param sid the Nuxeo Core session id.
     */
    void delSession(String sid) throws ClientException;

    /**
     * Return a session given its Nuxeo Core session id if exists.
     *
     * @param sid the Nuxeo Core session id.
     * @return a <code>WSRemotingSession</code> instance.
     * @throws ClientException if the session does not exist anymore.
     */
    WSRemotingSession getSession(String sid) throws ClientException;

}
