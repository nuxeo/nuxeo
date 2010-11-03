/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.session;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SessionProvider {

    protected Map<String, CoreSession> sessions;

    public SessionProvider() {
        sessions = new HashMap<String, CoreSession>();
    }

    public CoreSession getSession(String repositoryName) throws Exception {
        CoreSession session = sessions.get(repositoryName);
        if (session == null) {
            synchronized (this) {
                session = sessions.get(repositoryName);
                if (session == null) {
                    session = openSession(repositoryName);
                    sessions.put(repositoryName, session);
                }
            }
        }
        return session;
    }

    public CoreSession openSession(String repoName) throws Exception {
        RepositoryManager rm = Framework.getService(RepositoryManager.class);
        Repository repo = null;
        if (repoName == null) {
            repo = rm.getDefaultRepository();
        } else {
            repo = rm.getRepository(repoName);
        }
        if (repo == null) {
            throw new SessionException("Unable to get " + repoName
                    + " repository");
        }
        return repo.open();
    }

    public synchronized void destroy(Principal principal) throws Exception {
        if (sessions == null || sessions.isEmpty()) {
            sessions = null;
            return;
        }
        LoginContext lc = null;
        try {
            lc = Framework.loginAs(principal.getName());
            for (CoreSession session : sessions.values()) {
                session.destroy();
            }
            sessions = null;
        } finally {
            if (lc != null) {
                try {
                    lc.logout();
                } catch (LoginException e) {
                }
            }
        }

    }

}
