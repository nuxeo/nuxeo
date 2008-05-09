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

package org.nuxeo.ecm.platform.ui.web.auth.proxy;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

public class ProxyAuthenticator implements NuxeoAuthenticationPlugin {

    private static String HEADER_NAME_KEY = "ssoHeaderName";

    protected String userIdHeaderName = "remote_user";

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return false;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String userName = httpRequest.getHeader(userIdHeaderName);
        if (userName == null) {
            return null;
        }
        handleRedirectToValidStartPage(httpRequest, httpResponse);
        return new UserIdentificationInfo(userName, userName);
    }

    /**
     * Handle redirection so that context is rebuilt correctly
     *
     * see NXP-2060 + NXP-2064
     */
    protected void handleRedirectToValidStartPage(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        boolean isStartPageValid = false;
        if (httpRequest.getMethod().equals("GET")) {
            // try to keep valid start page
            isStartPageValid = NuxeoAuthenticationFilter.saveRequestedURLBeforeRedirect(
                    httpRequest, httpResponse);
        }
        HttpSession session;
        if (httpResponse.isCommitted()) {
            session = httpRequest.getSession(false);
        } else {
            session = httpRequest.getSession(true);
        }
        if (session != null && !isStartPageValid) {
            session.setAttribute(NuxeoAuthenticationFilter.START_PAGE_SAVE_KEY,
                    NuxeoAuthenticationFilter.DEFAULT_START_PAGE
                            + "?loginRedirection=true");
        }
    }

    public void initPlugin(Map<String, String> parameters) {
        if (parameters.containsKey(HEADER_NAME_KEY))
            userIdHeaderName = parameters.get(HEADER_NAME_KEY);
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return false;
    }

}
