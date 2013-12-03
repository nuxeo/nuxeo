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
package org.nuxeo.ecm.webdav.backend;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webdav.service.WIRequestFilter;
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

        Backend backend;
        if (request != null) {
            backend = (Backend) request.getAttribute(WIRequestFilter.BACKEND_KEY);
            if (backend == null) {
                backend = createRootBackend();
                request.setAttribute(WIRequestFilter.BACKEND_KEY, backend);
            }
            // register a backend cleanup handler
            // unless there's none (pure servlet call for WSS)
            RequestContext activeContext = RequestContext.getActiveContext(request);
            if (activeContext != null) {
                final Backend be = backend;
                activeContext.addRequestCleanupHandler(new RequestCleanupHandler() {
                    @Override
                    public void cleanup(HttpServletRequest req) {
                        be.destroy();
                    }
                });
            }
        } else {
            // for tests
            backend = createRootBackend();
        }
        return backend.getBackend(path);
    }

    protected abstract Backend createRootBackend();

}
