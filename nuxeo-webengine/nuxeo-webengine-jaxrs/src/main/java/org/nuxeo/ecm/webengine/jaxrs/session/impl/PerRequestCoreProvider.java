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
package org.nuxeo.ecm.webengine.jaxrs.session.impl;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.session.CoreSessionProvider;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionRef;
import org.nuxeo.ecm.webengine.jaxrs.session.impl.PerRequestCoreProvider.Ref;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PerRequestCoreProvider extends CoreSessionProvider<Ref> {

    public static class Ref implements SessionRef {
        protected CoreSession session;
        public Ref(CoreSession session) {
            this.session = session;
        }
        @Override
        public CoreSession get() {
            return session;
        }
        @Override
        public void unget() {
            // do nothing
        }
        public void destroy() {
            try {
                session.destroy();
            } finally {
                session = null;
            }
        }
    }


    @Override
    protected void onRequestDone(HttpServletRequest request) {
        // destroy all sessions created during this request
        if (!sessions.isEmpty()) {
            for (SessionRef ref : getSessions()) {
                ref.destroy();
            }
        }
        sessions = null;
    }

    @Override
    protected Ref createSessionRef(CoreSession session) {
        return new Ref(session);
    }



}
