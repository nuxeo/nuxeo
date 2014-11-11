/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api.local;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.TransactionalCoreSessionWrapper;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * Local Session: a CoreSession not tied to an EJB container.
 */
public class LocalSession extends AbstractSession {

    private static final long serialVersionUID = 1L;

    private Session session;

    private Boolean supportsTags;

    public static CoreSession createInstance() {
        CoreSession session = new LocalSession();
        return TransactionalCoreSessionWrapper.wrap(session);
    }

    // Locally we don't yet support NXCore.getRepository()
    protected Session createSession(String repoName) throws ClientException {
        try {
            NuxeoPrincipal principal = null;
            if (sessionContext != null) {
                principal = (NuxeoPrincipal) sessionContext.get("principal");
                if (principal == null) {
                    String username = (String) sessionContext.get("username");
                    if (username != null) {
                        if (SYSTEM_USERNAME.equals(username)) {
                            principal = new SystemPrincipal(null);
                        } else {
                            principal = new UserPrincipal(username,
                                    new ArrayList<String>(), false, false);
                        }
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
                        principal = (NuxeoPrincipal) p;
                    } else if (LoginComponent.isSystemLogin(p)) {
                        principal = new SystemPrincipal(p.getName());
                    } else {
                        throw new Error("Unsupported principal: "
                                + p.getClass());
                    }
                }
            }
            if (principal == null) {
                if (isTestingContext()) {
                    principal = new SystemPrincipal(null);
                } else {
                    throw new ClientException(
                            "Cannot create a core session outside a security context. You must login first.");
                }
            }
            // store the principal in the core session context so that other
            // core tools may retrieve it
            sessionContext.put("principal", principal);

            Repository repo = lookupRepository(repoName);
            supportsTags = Boolean.valueOf(repo.supportsTags());
            return repo.getSession(sessionContext);
        } catch (Exception e) {
            throw new ClientException("Failed to load repository " + repoName,
                    e);
        }
    }

    @Override
    public boolean supportsTags(String repositoryName) throws ClientException {
        try {
            Repository repo = lookupRepository(repositoryName);
            return repo.supportsTags();
        } catch (Exception e) {
            throw new ClientException("Failed to load repository "
                    + repositoryName, e);
        }
    }

    @Override
    public boolean supportsTags() throws ClientException {
        if (supportsTags != null) {
            return supportsTags.booleanValue();
        }
        throw new ClientException("Can not query on a closed repository");
    }

    protected Repository lookupRepository(String name) throws Exception {
        Repository repo;
        try {
            // needed by glassfish
            repo = (Repository) new InitialContext().lookup("NXRepository/"
                    + name);
        } catch (NamingException e) {
            try {
                // needed by jboss
                repo = (Repository) new InitialContext().lookup("java:NXRepository/"
                        + name);
            } catch (NamingException ee) {
                repo = NXCore.getRepositoryService().getRepositoryManager().getRepository(
                        name);
            }
        }
        if (repo == null) {
            throw new IllegalArgumentException("Repository not found: " + name);
        }
        return repo;
    }

    /**
     * This method is for compatibility with < 1.5 core In older core this
     * class were used only for testing - but now it is used by webengine and a
     * security fix that break tests was done. This method is checking if we
     * are in a testing context
     */
    public boolean isTestingContext() { // neither in jboss nor in nuxeo
        // launcher
        return Framework.isTestModeSet();
    }

    @Override
    public Principal getPrincipal() {
        return (Principal) sessionContext.get("principal");
    }

    @Override
    public Session getSession() throws ClientException {
        if (session == null || !session.isLive()) {
            session = createSession(repositoryName);
        }
        return session;
    }

    @Override
    public void cancel() throws ClientException {
        if (session != null && session.isLive()) {
            super.cancel();
        }
    }

    @Override
    public boolean isStateSharedByAllThreadSessions() {
        // each new LocalSession has its own state even in the same thread
        return false;
    }

    @Override
    public boolean isSessionAlive() {
        return session != null && session.isLive();
    }

}
