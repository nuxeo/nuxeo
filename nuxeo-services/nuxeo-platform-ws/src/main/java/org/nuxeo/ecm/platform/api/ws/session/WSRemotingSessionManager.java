/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     anguenot
 *
 * $Id: WSRemotingSessionManager.java 19483 2007-05-27 10:52:56Z sfermigier $
 */

package org.nuxeo.ecm.platform.api.ws.session;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Web service remoting session manager.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
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
     * @return an intialized <code>WSRemotingSession</code> instance.
     */
    WSRemotingSession createSession(String username, String password, String repository, UserManager um,
            CoreSession session);

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
    void delSession(String sid);

    /**
     * Return a session given its Nuxeo Core session id if exists.
     *
     * @param sid the Nuxeo Core session id.
     * @return a <code>WSRemotingSession</code> instance.
     */
    WSRemotingSession getSession(String sid);

}
