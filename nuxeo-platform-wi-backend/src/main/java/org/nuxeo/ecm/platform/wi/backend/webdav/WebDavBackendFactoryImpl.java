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
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.platform.wi.backend.webdav;

import org.nuxeo.ecm.platform.wi.backend.Backend;
import org.nuxeo.ecm.platform.wi.backend.BackendFactory;
import org.nuxeo.ecm.platform.wi.service.PluggableBackendFactory;
import org.nuxeo.ecm.webdav.backend.WebDavBackend;
import org.nuxeo.ecm.webdav.backend.WebDavBackendFactory;

import javax.servlet.http.HttpServletRequest;

public class WebDavBackendFactoryImpl implements WebDavBackendFactory {

    private BackendFactory factory = new PluggableBackendFactory();

    @Override
    public WebDavBackend getBackend(String path,
            HttpServletRequest httpServletRequest) {
        Backend backend = factory.getBackend(path, httpServletRequest);
        if (backend == null) {
            return null;
        }
        return new WebDavBackendAdapter(backend);
    }
}
