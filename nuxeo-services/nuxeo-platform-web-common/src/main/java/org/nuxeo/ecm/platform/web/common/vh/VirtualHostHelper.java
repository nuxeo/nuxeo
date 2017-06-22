/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.web.common.vh;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.REQUESTED_URL;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

public class VirtualHostHelper {

    private static final int HTTP_PORT_NUMBER = 80;

    private static final int HTTPS_PORT_NUMBER = 443;

    private static final Log log = LogFactory.getLog(VirtualHostHelper.class);

    private static final String X_FORWARDED_HOST = "x-forwarded-host";

    private static final String X_FORWARDED_PROTO = "x-forwarded-proto";

    private static final String X_FORWARDED_PORT = "x-forwarded-port";

    private static final String VH_HEADER = "nuxeo-virtual-host";

    private static final String VH_PARAM = "nuxeo.virtual.host";

    // Utility class.
    private VirtualHostHelper() {
    }

    private static HttpServletRequest getHttpServletRequest(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            return (HttpServletRequest) request;
        }
        return null;
    }

    /**
     * @return WebApp name : ie : nuxeo
     */
    public static String getWebAppName(ServletRequest request) {
        return getContextPath(request).replace("/", "");
    }

    /**
     * @return Server URL as : protocol://serverName:port/
     */
    public static String getServerURL(ServletRequest request) {
        return getServerURL(request, false);
    }

    private static String getServerUrl(String scheme, String serverName, int serverPort) {
        StringBuilder sbaseURL = new StringBuilder();
        sbaseURL.append(scheme);
        sbaseURL.append("://");
        sbaseURL.append(serverName);
        if (serverPort != 0) {
            if ("http".equals(scheme) && serverPort != HTTP_PORT_NUMBER || "https".equals(scheme)
                    && serverPort != HTTPS_PORT_NUMBER) {
                sbaseURL.append(':');
                sbaseURL.append(serverPort);
            }
        }
        sbaseURL.append('/');
        return sbaseURL.toString();
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
            if (nuxeoVH == null) {
                nuxeoVH = Framework.getProperty(VH_PARAM);
            }
            if (!local && nuxeoVH != null && nuxeoVH.contains("http")) {
                baseURL = nuxeoVH;
            } else {
                // default values
                String serverName = httpRequest.getServerName();
                int serverPort = httpRequest.getServerPort();
                String scheme = httpRequest.getScheme();

                if (!local) {
                    String forwardedPort = httpRequest.getHeader(X_FORWARDED_PORT);

                    if (forwardedPort != null) {
                        try {
                            serverPort = Integer.parseInt(forwardedPort);
                        } catch (NumberFormatException e) {
                            log.error("Unable to get forwarded port from header", e);
                        }
                    }

                    String forwardedProto = httpRequest.getHeader(X_FORWARDED_PROTO);
                    if (forwardedProto != null) {
                        scheme = forwardedProto;
                    }

                    // Detect virtual hosting based in standard header
                    String forwardedHost = httpRequest.getHeader(X_FORWARDED_HOST);
                    if (forwardedHost != null) {
                        if (forwardedHost.contains(":")) {
                            serverName = forwardedHost.split(":")[0];
                            serverPort = Integer.valueOf(forwardedHost.split(":")[1]);
                        } else {
                            serverName = forwardedHost;
                            serverPort = "https".equals(scheme) ? HTTPS_PORT_NUMBER : HTTP_PORT_NUMBER;
                        }
                    }
                }

                baseURL = getServerUrl(scheme, serverName, serverPort);
            }
        }
        if (baseURL == null) {
            log.error("Could not retrieve base url correctly");
            log.debug("Could not retrieve base url correctly", new Throwable());
        }
        return baseURL;
    }

    /**
     * @return base URL as protocol://serverName:port/webappName/
     */
    public static String getBaseURL(ServletRequest request) {
        String baseURL = null;
        String serverUrl = getServerURL(request, false);
        if (serverUrl != null) {
            String webAppName = getWebAppName(request);

            baseURL = StringUtils.isNotBlank(webAppName) ? serverUrl + webAppName + '/' : serverUrl;

        }
        return baseURL;
    }

    /**
     * Returns the context path of the application. Try to get it from the {@code ServletRequest} and then from the
     * {@code org.nuxeo.ecm.contextPath} system property. Fallback on default context path {@code /nuxeo}.
     */
    public static String getContextPath(ServletRequest request) {
        HttpServletRequest httpRequest = getHttpServletRequest(request);
        String contextPath = null;
        if (httpRequest != null) {
            contextPath = httpRequest.getContextPath();
        }
        return contextPath != null ? contextPath : getContextPathProperty();
    }

    public static String getContextPathProperty() {
        return Framework.getProperty("org.nuxeo.ecm.contextPath", "/nuxeo");
    }

    /**
     * Computes the url to be redirected when logging out
     * 
     * @return redirect URL as protocol://serverName:port/webappName/...
     * @since 9.1
     */
    public static String getRedirectUrl(HttpServletRequest request) {
        String redirectURL = getBaseURL(request);
        if (request.getAttribute(REQUESTED_URL) != null) {
            redirectURL += request.getAttribute(REQUESTED_URL);
        } else if (request.getParameter(REQUESTED_URL) != null) {
            redirectURL += request.getParameter(REQUESTED_URL);
        } else {
            redirectURL = request.getRequestURL().toString();
            String queryString = request.getQueryString();
            if (queryString != null) {
                redirectURL += '?' + queryString;
            }
        }
        return redirectURL;
    }

}
