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
 * $Id: WSRemotingSessionManagerImpl.java 21703 2007-07-01 20:48:16Z sfermigier $
 */

package org.nuxeo.ecm.platform.ws.session;

import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSessionManager;
import org.nuxeo.ecm.platform.api.ws.session.impl.WSRemotingSessionImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Web service Remoting session manager implemtation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class WSRemotingSessionManagerImpl extends DefaultComponent implements
        WSRemotingSessionManager {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.ws.session.WSRemotingSessionManagerImpl");

    private static final Log log = LogFactory.getLog(WSRemotingSessionManagerImpl.class);

    private static final Map<String, WSRemotingSession> sessions
            = new Hashtable<String, WSRemotingSession>();


    public void addSession(String sid, WSRemotingSession session) {
        log.debug("Adding a new Web Service remoting session for username="
                + session.getUsername());
        sessions.put(sid, session);
    }

    public WSRemotingSession createSession(String username, String password,
            String repository, UserManager um, CoreSession session) {
        return new WSRemotingSessionImpl(session, um, repository, username,
                password);
    }

    public WSRemotingSession getSession(String sid) throws ClientException {
        if (sid == null) {
            throw new ClientException("Invalid value for sid... null value");
        }
        WSRemotingSession session = sessions.get(sid);
        if (session == null) {
            throw new ClientException("Cannot find session for sid=" + sid);
        }
        log.debug("Found session for username=" + session.getUsername());
        log.debug("Forwarding the session now...");
        return session;
    }

    public void delSession(String sid) throws ClientException {
        // Throw a ClientException if session not found.
        WSRemotingSession session = getSession(sid);
        sessions.remove(sid);
        log.debug("Removing session for username=" + session.getUsername());
    }

}
