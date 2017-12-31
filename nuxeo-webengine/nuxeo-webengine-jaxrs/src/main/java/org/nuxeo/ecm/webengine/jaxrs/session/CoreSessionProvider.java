/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.session;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
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

    protected abstract REF createSessionRef(CloseableCoreSession session);

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

    protected CloseableCoreSession createSession(HttpServletRequest request, String repoName) {
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
