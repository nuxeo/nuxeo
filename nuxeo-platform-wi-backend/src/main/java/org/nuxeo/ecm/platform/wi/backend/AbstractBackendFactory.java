/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.platform.wi.backend;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.wi.filter.WIRequestFilter;
import org.nuxeo.ecm.platform.wi.filter.WISession;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;

public abstract class AbstractBackendFactory implements BackendFactory {

    private static final Log log = LogFactory.getLog(AbstractBackendFactory.class);

    @Override
    public Backend getBackend(String path, HttpServletRequest request) {

        if (log.isDebugEnabled() && request != null) {
            log.debug("Get backend for method:" + request.getMethod() + " uri:"
                    + request.getRequestURI());
        }

        final Backend backend;
        if (request != null) {
            WISession wiSession = (WISession) request.getAttribute(WIRequestFilter.SESSION_KEY);
            backend = getBackend(wiSession);
            // register a backend cleanup handler
            // unless there's none (pure servlet call for WSS)
            RequestContext activeContext = RequestContext.getActiveContext(request);
            if (activeContext != null) {
                activeContext.addRequestCleanupHandler(new RequestCleanupHandler() {
                    @Override
                    public void cleanup(HttpServletRequest req) {
                        backend.destroy();
                    }
                });
            }
        } else {
            // for tests
            backend = createRootBackend();
        }
        return backend.getBackend(path);
    }

    @Override
    public Backend getBackend(WISession wiSession) {

        Backend backend = null;
        if (wiSession != null) {
            Object sessionBackend = wiSession.getAttribute(WISession.BACKEND_KEY);
            if (sessionBackend != null) {
                backend = (Backend) sessionBackend;
            }
        }

        if (backend == null) {
            CoreSession session = null;
            if (wiSession != null) {
                session = (CoreSession) wiSession.getAttribute(WISession.CORESESSION_KEY);
            }
            backend = createRootBackend();
            if (session != null) {
                backend.setSession(session);
            }
        }

        if (wiSession != null) {
            wiSession.setAttribute(WISession.BACKEND_KEY, backend);
        }

        return backend;
    }

    protected abstract Backend createRootBackend();

}
