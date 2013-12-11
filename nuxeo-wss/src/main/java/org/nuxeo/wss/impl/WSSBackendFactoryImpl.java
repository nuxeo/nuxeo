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
package org.nuxeo.wss.impl;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.ecm.webdav.backend.BackendHelper;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSBackendFactory;

public class WSSBackendFactoryImpl implements WSSBackendFactory {

    protected String computeVirtualRoot(WSSRequest request) {
        if(request == null){
            return "nuxeo"; //for tests
        }

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
        String path = "/";
        HttpServletRequest request = wssRequest == null ? null : wssRequest.getHttpRequest();
        Backend backend = BackendHelper.getBackend(path, request);
        if (backend == null) {
            return new WSSFakeBackend();
        }

        String virtualRoot = computeVirtualRoot(wssRequest);

        if (backend.isRoot()) {
            return new WSSRootBackendAdapter(backend, virtualRoot);
        } else if (backend.isVirtual()) {
            return new WSSVirtualBackendAdapter(backend, virtualRoot);
        } else {
            return new WSSBackendAdapter(backend, virtualRoot);
        }
    }

}
