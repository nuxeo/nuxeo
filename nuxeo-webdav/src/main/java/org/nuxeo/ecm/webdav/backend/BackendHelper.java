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
package org.nuxeo.ecm.webdav.backend;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.webdav.service.WebDavService;

public class BackendHelper {

    /**
     * For tests. Otherwise the factory is configured through an extension point in the component.
     */
    public static void setBackendFactory(BackendFactory backendFactory) {
        WebDavService.instance().setBackendFactory(backendFactory);
    }

    public static Backend getBackend(String path, HttpServletRequest request) {
        return WebDavService.instance().getBackendFactory().getBackend(path, request);
    }

}
