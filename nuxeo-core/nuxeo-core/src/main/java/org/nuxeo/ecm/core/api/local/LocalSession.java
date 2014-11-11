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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.local;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LocalSession extends AbstractSession {

    private static final long serialVersionUID = 1L;

    private Session session;

    private Boolean supportsTags = null;

    // Locally we don't yet support NXCore.getRepository()
    protected Session createSession(String repoName) throws ClientException {
        try {
            NuxeoPrincipal principal = null;
            if (sessionContext != null) {
                principal = (NuxeoPrincipal) sessionContext.get("principal");
                if (principal == null) {
                    String username = (String) sessionContext.get("username");
                    if (username != null) {
                        principal = new UserPrincipal(username);
                    }
                }
            } else {
                sessionContext = new HashMap<String, Serializable>();
            }
            if (principal == null) {
                LoginStack.Entry entry = ClientLoginModule.getCurrentLogin();
                if (entry != null) {
                    Principal p = entry.getPrincipal();
                    if (p instanceof NuxeoPrincipal) {
                        principal = (NuxeoPrincipal)p;
                    } else if (LoginComponent.isSystemLogin(p)) {
                     // TODO: must use SystemPrincipal from nuxeo-platform-login
                        principal = new UserPrincipal("system");
                    } else {
                        throw new Error("Unsupported principal: "+p.getClass());
                    }
                }
            }
            if (principal == null && isTestingContext()) {
                principal = new UserPrincipal("system");
            }
            // store the principal in the core session context so that other core tools may retrieve it
            sessionContext.put("principal", principal);

            Repository repo = lookupRepository(repoName);
            supportsTags = repo.supportsTags();
            return repo.getSession(sessionContext);
        } catch (Exception e) {
            throw new ClientException("Failed to load repository " + repoName, e);
        }
    }

    public boolean supportsTags(String repositoryName) throws ClientException {
        try {
            Repository repo = lookupRepository(repositoryName);
            return repo.supportsTags();
        } catch (Exception e) {
            throw new ClientException("Failed to load repository " + repositoryName, e);
        }
    }

    public boolean supportsTags() throws ClientException {
        if (supportsTags!=null) {
            return supportsTags.booleanValue();
        }
        throw new ClientException("Can not query on a closed repository");
    }

    protected Repository lookupRepository(String name) throws Exception {
        try {
            //needed by glassfish
            return (Repository) new InitialContext()
            .lookup("NXRepository/" + name);
        } catch (NamingException e) {
            return NXCore.getRepositoryService()
            .getRepositoryManager().getRepository(name);
        }
    }

    /**
     * This method is for compatibility with < 1.5 core
     * In older core this class were used only for testing - but now it is used by webengine
     * and a security fix that break tests was done.
     * This method is checking if we are in a testing context
     * @return
     */
    public boolean isTestingContext() { // neither in jboss neither in nuxeo launcher
        return Framework.isTestModeSet();
    }

    @Override
    public Principal getPrincipal() {
        return (Principal)sessionContext.get("principal");
    }

    @Override
    public Session getSession() throws ClientException {
        if (session == null) {
            session = createSession(repositoryName);
        }
        return session;
    }

    public boolean isStateSharedByAllThreadSessions() {
        // each new LocalSession has its own state even in the same thread
        return false;
    }

    @Override
    public boolean isSessionAlive() {
        return session != null && session.isLive();
    }

}
