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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;

public class Backend {

protected static WebDavBackendFactory factoryWebDav = null;

    private static final Log log = LogFactory.getLog(WebDavBackendFactory.class);

    public static WebDavBackendFactory getFactory() {
        if (factoryWebDav == null) {
            factoryWebDav = loadFactory();
        }
        return factoryWebDav;
    }

    protected synchronized static WebDavBackendFactory loadFactory() {
        String factoryClass = "org.nuxeo.ecm.platform.wi.backend.webdav.WebDavBackendFactoryImpl";
        try {
            factoryWebDav = (WebDavBackendFactory) Class.forName(factoryClass, true, Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (Exception e) {
            log.error("Unable to create backend factoryWebDav", e);
        }
        return factoryWebDav;
    }

    public static WebDavBackend get(String path, HttpServletRequest request) {
        return getFactory().getBackend(path, request);
    }

}
