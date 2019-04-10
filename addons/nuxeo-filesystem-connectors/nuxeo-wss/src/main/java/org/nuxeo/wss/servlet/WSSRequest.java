/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.wss.servlet;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.wss.WSSConfig;

public class WSSRequest {

    protected HttpServletRequest httpRequest;

    protected String sitePath;

    public WSSRequest(HttpServletRequest httpRequest, String sitePath) {
        this.httpRequest = httpRequest;
        this.sitePath = sitePath;
    }

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    public String getBaseUrl() {
        return getBaseUrl(null);
    }

    public String getBaseUrl(String fpDir) {

        if (VHHelper.isVirtualHosted(getHttpRequest())) {
            return VHHelper.getServerURL(getHttpRequest(), false);
        } else {

            StringBuffer base = new StringBuffer();

            base.append(getHttpRequest().getScheme());
            base.append("://");
            base.append(getHttpRequest().getServerName());

            if (getHttpRequest().getServerPort() != 80) {
                base.append(":");
                base.append(getHttpRequest().getServerPort());
            }

            base.append("/");
            return base.toString();
        }
    }

    public String getResourcesUrl() {
        StringBuffer base = new StringBuffer();

        base.append(getBaseUrl());
        base.append(WSSConfig.instance().getWSSUrlPrefix());
        String resourcePattern = WSSConfig.instance().getResourcesUrlPattern();
        if (!resourcePattern.startsWith("/")) {
            base.append("/");
        }
        base.append(WSSConfig.instance().getResourcesUrlPattern());
        if (!resourcePattern.endsWith("/")) {
            base.append("/");
        }
        return base.toString();
    }

    public String getUserName() {
        Principal principal = getHttpRequest().getUserPrincipal();
        if (principal == null) {
            return "";
        } else {
            return principal.getName();
        }
    }

    public String getSitePath() {
        return sitePath;
    }

}
