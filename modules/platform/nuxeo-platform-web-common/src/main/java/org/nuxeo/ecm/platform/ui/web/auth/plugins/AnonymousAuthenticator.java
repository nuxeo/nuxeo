/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *
 * $Id: AnonymousAuthenticator.java 30865 2008-03-11 09:00:53Z arussel $
 */

package org.nuxeo.ecm.platform.ui.web.auth.plugins;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Thierry Delprat
 */
public class AnonymousAuthenticator implements NuxeoAuthenticationPlugin, NuxeoAuthenticationPluginLogoutExtension {

    public static final String BLOCK_ANONYMOUS_LOGIN_KEY = "org.nuxeo.ecm.platform.ui.web.auth.anonymous.block";

    private static final Log log = LogFactory.getLog(AnonymousAuthenticator.class);

    protected boolean initialized;

    protected String anonymousLogin;

    // Called by JSP page
    public static boolean isAnonymousRequest(HttpServletRequest httpRequest) {
        Principal user = httpRequest.getUserPrincipal();
        if (user != null && user instanceof NuxeoPrincipal) {
            return ((NuxeoPrincipal) user).isAnonymous();
        }
        return false;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (!initialized) {
            UserManager userManager = Framework.getService(UserManager.class);
            if (userManager != null) {
                anonymousLogin = userManager.getAnonymousUserId();
            }
            initialized = true;
        }
        if (anonymousLogin == null) {
            return null;
        }

        if (isAnonymousLoginBlocked(httpRequest)) {
            return null;
        }

        return new UserIdentificationInfo(anonymousLogin);
    }

    protected boolean isAnonymousLoginBlocked(HttpServletRequest httpRequest) {
        if (Boolean.TRUE.equals(httpRequest.getAttribute(BLOCK_ANONYMOUS_LOGIN_KEY))) {
            httpRequest.removeAttribute(BLOCK_ANONYMOUS_LOGIN_KEY);
            return true;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(BLOCK_ANONYMOUS_LOGIN_KEY))) {
            // next logout will clear the session anyway !!
            // session.setAttribute(BLOCK_ANONYMOUS_LOGIN_KEY, false);
            return true;
        }
        return false;
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {
        // NOP
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return Boolean.FALSE;
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        return null;
    }

    @Override
    public Boolean handleLogout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return Boolean.FALSE;
    }

}
