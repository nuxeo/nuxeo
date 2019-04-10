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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webdav.service.WIRequestFilter;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;

public abstract class AbstractBackendFactory implements BackendFactory {

    @Override
    public Backend getBackend(String path, HttpServletRequest request) {
        if (request == null) {
            throw new NullPointerException("null request");
        }
        Backend backend = (Backend) request.getAttribute(WIRequestFilter.BACKEND_KEY);
        if (backend == null) {
            // create backend from WebEngine session
            WebContext webContext = WebEngine.getActiveContext();
            if (webContext == null) {
                throw new NullPointerException("null WebContext");
            }
            CoreSession session = webContext.getCoreSession();
            if (session == null) {
                throw new NullPointerException("null CoreSession");
            }
            backend = createRootBackend(session);
            request.setAttribute(WIRequestFilter.BACKEND_KEY, backend);
        }
        return backend.getBackend(path);
    }

    public abstract Backend createRootBackend(CoreSession session);

}
