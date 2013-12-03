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
 */
package org.nuxeo.ecm.webdav.service;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.ecm.webdav.backend.BackendFactory;
import org.nuxeo.runtime.api.Framework;

public class PluggableBackendFactory implements BackendFactory {

    private BackendFactory factory;

    private BackendFactory getBackendFactory() {
        if (factory == null) {
            WebDavService manager = (WebDavService) Framework.getRuntime().getComponent(
                    WebDavService.NAME);
            factory = manager.getBackendFactory();
        }
        return factory;
    }

    @Override
    public Backend getBackend(String path, HttpServletRequest request) {
        return getBackendFactory().getBackend(path, request);
    }

    @Override
    public Backend getBackend(WISession wiSession) {
        return getBackendFactory().getBackend(wiSession);
    }
}
