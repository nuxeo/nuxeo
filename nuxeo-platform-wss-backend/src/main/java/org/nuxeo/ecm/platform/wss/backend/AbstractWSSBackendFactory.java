/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.wss.backend;

import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSBackendFactory;

public abstract class AbstractWSSBackendFactory implements WSSBackendFactory {

    public static final String BACKEND_KEY = "org.nuxeo.ecm.platform.wss.backend";

    protected String computeVirtualRoot(WSSRequest request) {
        String virtualRoot = null;
        if (request == null) { // happens during unit tests
            virtualRoot = System.getProperty("org.nuxeo.ecm.contextPath", "/nuxeo");
        } else {
            virtualRoot = request.getSitePath();
        }
        if (virtualRoot == null || virtualRoot.equals("")) {
            virtualRoot = request.getHttpRequest().getContextPath();
        }
        if (virtualRoot.startsWith("/")) {
            virtualRoot = virtualRoot.substring(1);
        }
        return virtualRoot;
    }

    public WSSBackend getBackend(WSSRequest request) {

        if (request == null) {
            return createBackend(null); // for tests
        }

        // force session creation to avoid auth issues
        request.getHttpRequest().getSession(true);

        Object object = request.getHttpRequest().getAttribute(BACKEND_KEY);
        if (object != null) {
            return (WSSBackend) object;
        } else {
            WSSBackend backend = createBackend(request);
            request.getHttpRequest().setAttribute(BACKEND_KEY, backend);
            return backend;
        }
    }

    protected abstract WSSBackend createBackend(WSSRequest request);

}
