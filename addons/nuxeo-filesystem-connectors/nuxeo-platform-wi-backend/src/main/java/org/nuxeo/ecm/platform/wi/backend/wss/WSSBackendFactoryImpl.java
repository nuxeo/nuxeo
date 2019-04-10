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
package org.nuxeo.ecm.platform.wi.backend.wss;

import org.nuxeo.ecm.platform.wi.backend.Backend;
import org.nuxeo.ecm.platform.wi.backend.BackendFactory;
import org.nuxeo.ecm.platform.wi.service.PluggableBackendFactory;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSBackendFactory;

public class WSSBackendFactoryImpl implements WSSBackendFactory {

    private BackendFactory factory = new PluggableBackendFactory();

    // for tests
    public void setFactory(BackendFactory factory) {
        this.factory = factory;
    }

    protected String computeVirtualRoot(WSSRequest request) {
        String virtualRoot = request.getSitePath();

        if (virtualRoot == null || virtualRoot.equals("")) {
            virtualRoot = request.getHttpRequest().getContextPath();
        }
        if (virtualRoot.startsWith("/")) {
            virtualRoot = virtualRoot.substring(1);
        }
        return virtualRoot;
    }

    @Override
    public WSSBackend getBackend(WSSRequest wssRequest) {
        String virtualRoot;
        if (wssRequest != null) {
            virtualRoot = computeVirtualRoot(wssRequest);
        } else {
            virtualRoot = "nuxeo";
        }
        Backend backend = null;
        if (wssRequest != null) {
            backend = factory.getBackend("/", wssRequest.getHttpRequest());
        } else {
            backend = factory.getBackend("/", null);
        }
        if (backend == null) {
            return new WSSFakeBackend();
        }
        if (backend.isRoot()) {
            return new WSSRootBackendAdapter(backend, virtualRoot);
        } else if (backend.isVirtual()) {
            return new WSSVirtualBackendAdapter(backend, virtualRoot);
        } else {
            return new WSSBackendAdapter(backend, virtualRoot);
        }
    }

}
