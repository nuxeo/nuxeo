/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.session;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class CoreSessionProvider<REF extends SessionRef> {

    protected Map<String, REF> sessions;

    protected CoreSessionProvider() {
        this.sessions = new HashMap<String, REF>();
    }

    /**
     * The HTTP request was consumed. Do any request level cleanup now.
     */
    protected abstract void onRequestDone(HttpServletRequest request);

    protected abstract REF createSessionRef(CoreSession session);

    public SessionRef[] getSessions() {
        return sessions.values().toArray(new SessionRef[sessions.size()]);
    }

    public SessionRef getSessionRef(HttpServletRequest request, String repoName) {
        REF ref = sessions.get(repoName);
        if (ref == null) {
            ref = createSessionRef(createSession(request, repoName));
            sessions.put(repoName, ref);
        }
        return ref;
    }

    public CoreSession getSession(HttpServletRequest request, String repoName) {
        return getSessionRef(request, repoName).get();
    }

    protected CoreSession createSession(HttpServletRequest request, String repoName) {
        try {
            return _createSession(request, repoName);
        } catch (ClientException e) {
            throw new WebApplicationException(e, 500);
        }
    }

    protected CoreSession _createSession(HttpServletRequest request, String repoName) throws ClientException {
        if (request.getUserPrincipal() == null) {
            throw new java.lang.IllegalStateException("Not authenticated user is trying to get a core session");
        }
        return CoreInstance.openCoreSession(repoName);
    }

    public boolean hasSessions() {
        return !sessions.isEmpty();
    }

    protected void destroy() {
        for (SessionRef ref : getSessions()) {
            ref.destroy();
        }
        sessions = null;
    }

}
