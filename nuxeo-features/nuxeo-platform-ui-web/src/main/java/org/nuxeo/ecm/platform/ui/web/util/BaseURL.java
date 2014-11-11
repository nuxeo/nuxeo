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
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class BaseURL {

    private static final Log log = LogFactory.getLog(BaseURL.class);

    private static final String X_FORWARDED_HOST = "x-forwarded-host";

    private static final String VH_HEADER = "nuxeo-virtual-host";

    private BaseURL() {
    }

    public static String getServerURL() {
        return getServerURL(null, false);
    }

    private static String getServerUrl(String scheme, String serverName,
            int serverPort) {
        StringBuilder sbaseURL = new StringBuilder();
        sbaseURL.append(scheme);
        sbaseURL.append("://");
        sbaseURL.append(serverName);
        if (serverPort != 0) {
            if ("http".equals(scheme) && serverPort != 80
                    || "https".equals(scheme) && serverPort != 443) {
                sbaseURL.append(':');
                sbaseURL.append(serverPort);
            }
        }
        sbaseURL.append('/');
        return sbaseURL.toString();
    }

    private static HttpServletRequest getHttpServletRequest(
            ServletRequest request) {
        if (request == null) {
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                request = (ServletRequest) facesContext.getExternalContext().getRequest();
            }
        }
        if (request instanceof HttpServletRequest) {
            return (HttpServletRequest) request;
        }
        return null;
    }

    /**
     * @return Server URL as : protocol://serverName:port/
     */
    public static String getServerURL(ServletRequest request, boolean local) {
        String baseURL = null;
        HttpServletRequest httpRequest = getHttpServletRequest(request);
        if (httpRequest != null) {
            // Detect Nuxeo specific header for VH
            String nuxeoVH = httpRequest.getHeader(VH_HEADER);
            if (!local && nuxeoVH != null && nuxeoVH.contains("http")) {
                baseURL = nuxeoVH;
            } else {
                // default values
                String serverName = httpRequest.getServerName();
                int serverPort = httpRequest.getServerPort();
                if (!local) {
                    // Detect virtual hosting based in standard header
                    String forwardedHost = httpRequest.getHeader(X_FORWARDED_HOST);
                    if (forwardedHost != null) {
                        if (forwardedHost.contains(":")) {
                            serverName = forwardedHost.split(":")[0];
                            serverPort = Integer.valueOf(forwardedHost.split(":")[1]);
                        } else {
                            serverName = forwardedHost;
                            serverPort = 80; // fallback
                        }
                    }
                }
                String scheme = httpRequest.getScheme();
                baseURL = getServerUrl(scheme, serverName, serverPort);
            }
        }
        if (baseURL == null) {
            log.error("Could not retrieve base url correctly");
        }
        return baseURL;
    }

    /**
     * @return WebApp name : ie : nuxeo
     */
    public static String getWebAppName() {
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String baseURL = facesContext.getExternalContext().getRequestContextPath();
            baseURL = baseURL.replace("/", "");
            return baseURL;
        } else {
            return "nuxeo";
        }
    }

    /**
     * @return base URL as protocol://serverName:port/webappName/
     */
    public static String getBaseURL() {
        String baseURL = null;
        String serverUrl = getServerURL();
        if (serverUrl != null) {
            baseURL = serverUrl + getWebAppName() + '/';
        }
        if (baseURL == null) {
            log.error("Could not retrieve base url correctly");
        }
        return baseURL;
    }

    public static String getBaseURL(ServletRequest request) {
        String baseURL = null;
        String serverUrl = getServerURL(request, false);
        if (serverUrl != null) {
            baseURL = serverUrl + getWebAppName() + '/';
        }
        if (baseURL == null) {
            log.error("Could not retrieve base url correctly");
        }
        return baseURL;
    }

    public static String getLocalBaseURL(ServletRequest request) {
        String localURL = null;
        String serverUrl = getServerURL(request, true);
        if (serverUrl != null) {
            localURL = serverUrl + getWebAppName() + '/';
        }
        if (localURL == null) {
            log.error("Could not retrieve loacl url correctly");
        }
        return localURL;
    }

}
