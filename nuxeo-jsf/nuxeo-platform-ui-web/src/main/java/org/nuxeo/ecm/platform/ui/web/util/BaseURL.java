/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

public final class BaseURL {

    private static final Log log = LogFactory.getLog(BaseURL.class);

    private BaseURL() {
    }

    static ServletRequest getRequest() {
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return null;
        }
        return (ServletRequest) facesContext.getExternalContext().getRequest();
    }

    public static String getServerURL() {
        return getServerURL(getRequest(), false);
    }

    /**
     * @return Server URL as: protocol://serverName:port/
     */
    public static String getServerURL(ServletRequest request, boolean local) {
        return VirtualHostHelper.getServerURL(request, local);
    }

    /**
     * @return WebApp name, ie  "nuxeo"
     */
    public static String getWebAppName() {
        ServletRequest request = getRequest();
        return VirtualHostHelper.getWebAppName(request);
    }

    /**
     * @return base URL as protocol://serverName:port/webappName/
     */
    public static String getBaseURL() {
        return getBaseURL(getRequest());
    }

    public static String getBaseURL(ServletRequest request) {
        return VirtualHostHelper.getBaseURL(request);
    }

    public static String getLocalBaseURL(ServletRequest request) {
        String localURL = null;
        String serverUrl = getServerURL(request, true);
        if (serverUrl != null) {
            localURL = serverUrl + getWebAppName() + '/';
        }
        if (localURL == null) {
            log.error("Could not retrieve local url correctly");
        }
        return localURL;
    }

    public static String getContextPath() {
        return VirtualHostHelper.getContextPath(getRequest());
    }

}
