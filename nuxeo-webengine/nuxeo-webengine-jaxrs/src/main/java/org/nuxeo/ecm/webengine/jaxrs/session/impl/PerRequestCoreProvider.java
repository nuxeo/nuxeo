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
package org.nuxeo.ecm.webengine.jaxrs.session.impl;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.session.CoreSessionProvider;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionRef;
import org.nuxeo.ecm.webengine.jaxrs.session.impl.PerRequestCoreProvider.Ref;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PerRequestCoreProvider extends CoreSessionProvider<Ref> {

    public static class Ref implements SessionRef {
        protected CloseableCoreSession session;

        public Ref(CloseableCoreSession session) {
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
                session.close();
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
    protected Ref createSessionRef(CloseableCoreSession session) {
        return new Ref(session);
    }

}
