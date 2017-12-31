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
 * $Id: AbstractNuxeoWebService.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ws;

import javax.annotation.security.PermitAll;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.api.ws.BaseNuxeoWebService;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSessionManager;
import org.nuxeo.ecm.platform.api.ws.session.impl.WSRemotingSessionImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract Nuxeo Web Service.
 * <p>
 * Extend this if you want to share the Web Service remoting sessions with the other Nuxeo platform web services.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractNuxeoWebService implements BaseNuxeoWebService {

    private static final long serialVersionUID = 5530614356404354863L;

    private WSRemotingSessionManager sessionsManager;

    /**
     * Returns the platform service that deals with shared Web Service remote sessions.
     *
     * @return a <code>WSRemotiungSessionManager</code> service
     */
    protected WSRemotingSessionManager getSessionsManager() {
        if (sessionsManager == null) {
            sessionsManager = Framework.getService(WSRemotingSessionManager.class);
        }
        return sessionsManager;
    }

    @PermitAll
    @WebMethod
    public String connectOnRepository(@WebParam(name = "userName") String username,
            @WebParam(name = "password") String password, @WebParam(name = "repositoryName") String repositoryName)
            {
        String sid = null;
        try {
            // :FIXME: won't work all the time...
            LoginContext loginContext = Framework.login();
            if (repositoryName == null) {
                RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
                repositoryName = repositoryManager.getDefaultRepositoryName();
            }
            sid = _connect(username, password, repositoryName);
            loginContext.logout();
        } catch (LoginException e) {
            throw new NuxeoException(e.getMessage(), e);
        }
        return sid;
    }

    @Override
    @PermitAll
    @WebMethod
    public String connect(@WebParam(name = "userName") String username, @WebParam(name = "password") String password)
            {
        return connectOnRepository(username, password, null);
    }

    /**
     * Internal connect method shared in between above connect() methods.
     *
     * @param username the user name.
     * @param password the user password.
     * @param repository the repository name.
     * @return a Nuxeo core session identifier.
     */
    private String _connect(String username, String password, String repositoryName) {
        // Login before doing anything.
        login(username, password);
        CoreSession session = CoreInstance.openCoreSession(repositoryName);
        String sid = session.getSessionId();
        UserManager userMgr = Framework.getService(UserManager.class);
        WSRemotingSession rs = new WSRemotingSessionImpl(session, userMgr, repositoryName, username, password);
        getSessionsManager().addSession(sid, rs);
        return sid;
    }

    @Override
    @WebMethod
    public void disconnect(@WebParam(name = "sessionId") String sid) {
        WSRemotingSession rs = initSession(sid);
        ((CloseableCoreSession) rs.getDocumentManager()).close();
    }

    /**
     * Initializes a new user session from the credentials bound the Web Service remote session.
     *
     */
    protected void login(String username, String password) {
        try {
            Framework.login(username, password);
        } catch (LoginException e) {
            throw new NuxeoException("Login failed for " + username, e);
        }
    }

    /**
     * Initializes a user session.
     *
     * @param sid the session identifier.
     * @return a Web Service remoting session instance.
     */
    protected WSRemotingSession initSession(String sid) {
        WSRemotingSession rs = getSessionsManager().getSession(sid);
        if (rs == null) {
            throw new NuxeoException("Invalid session id: " + sid);
        }
        login(rs.getUsername(), rs.getPassword());
        return rs;
    }

}
