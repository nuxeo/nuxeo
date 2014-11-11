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

package org.nuxeo.theme.jsf;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

public final class URLUtils {

    private static final Log log = LogFactory.getLog(URLUtils.class);

    private URLUtils() {
    }

    public static String getServerURL() {
        return getServerURL(null);
    }

    /**
     * @return Server URL as : protocol://serverName:port/
     */
    public static String getServerURL(ServletRequest request) {
        if (request == null) {
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            request = (ServletRequest) facesContext.getExternalContext().getRequest();
        }
        return VirtualHostHelper.getServerURL(request);
    }

    /**
     * @return WebApp name : ie : nuxeo
     */
    public static String getWebAppName() {
        return BaseURL.getWebAppName();
    }

    /**
     * @return base URL as protocol://serverName:port/webappName/
     */
    public static String getBaseURL() {
        return getBaseURL(null);
    }

    public static String getBaseURL(ServletRequest request) {
        String baseURL = null;
        String serverUrl = getServerURL(request);
        if (serverUrl != null) {
            baseURL = serverUrl + getWebAppName() + '/';
        }
        if (baseURL == null) {
            log.error("Could not retrieve base url correctly");
        }
        return baseURL;
    }

}
