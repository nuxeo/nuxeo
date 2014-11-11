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
