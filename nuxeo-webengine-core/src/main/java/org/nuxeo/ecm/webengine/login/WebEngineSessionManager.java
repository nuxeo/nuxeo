/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.webengine.login;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.DefaultSessionManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.session.StatefulUserSession;
import org.nuxeo.ecm.webengine.session.StatelessUserSession;
import org.nuxeo.ecm.webengine.session.UserSession;

public class WebEngineSessionManager extends DefaultSessionManager {

    // TODO work on skin request to avoid hardcoding paths
    private static final String RESOURCES_PATH = VirtualHostHelper.getContextPathProperty() + "/site/files/";
    private static final Log log = LogFactory
            .getLog(WebEngineSessionManager.class);

    @Override
    public boolean canBypassRequest(ServletRequest request) {
        // static resources don't require Authentication
        return ((HttpServletRequest) request).getRequestURI().startsWith(
                RESOURCES_PATH);
    }

    @Override
    public void onAuthenticatedSessionCreated(ServletRequest request,
            HttpSession httpSession,
            CachableUserIdentificationInfo cachebleUserInfo) {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // check for a valid session
        if (httpSession == null) {
            httpSession = httpRequest.getSession(false);
        }

        UserSession userSession;
        if (httpSession == null) {
            // create WE custom UserSession
            userSession = new StatelessUserSession(cachebleUserInfo.getPrincipal(),
                    cachebleUserInfo.getUserInfo().getPassword());
            log.debug("Creating Stateless UserSession");
        } else {
            // create WE custom UserSession
            userSession = new StatefulUserSession(cachebleUserInfo.getPrincipal(),
                    cachebleUserInfo.getUserInfo().getPassword());
            log.debug("Creating Stateful UserSession");
        }

        UserSession.register(httpRequest, userSession);
    }

    @Override
    public boolean needResetLogin(ServletRequest req) {
        return WebEngineFormAuthenticator.isLoginRequest((HttpServletRequest) req);
    }

}
