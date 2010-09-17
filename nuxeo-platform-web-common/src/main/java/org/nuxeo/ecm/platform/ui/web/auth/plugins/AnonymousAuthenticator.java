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
 *
 * @author Thierry Delprat
 */
public class AnonymousAuthenticator implements NuxeoAuthenticationPlugin,
        NuxeoAuthenticationPluginLogoutExtension {

    private static final Log log = LogFactory.getLog(AnonymousAuthenticator.class);

    public static final String BLOCK_ANONYMOUS_LOGIN_KEY = "org.nuxeo.ecm.platform.ui.web.auth.anonymous.block";

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

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (!initialized) {
            try {
                anonymousLogin = Framework.getService(UserManager.class).getAnonymousUserId();
            } catch (Exception e) {
                log.error(e, e);
            }
            initialized = true;
        }
        if (anonymousLogin == null) {
            return null;
        }

        if (isAnonymousLoginBlocked(httpRequest)) {
            return null;
        }

        return new UserIdentificationInfo(anonymousLogin, anonymousLogin);
    }

    protected boolean isAnonymousLoginBlocked(HttpServletRequest httpRequest) {
        if (Boolean.TRUE.equals(httpRequest.getAttribute(BLOCK_ANONYMOUS_LOGIN_KEY))) {
            httpRequest.removeAttribute(BLOCK_ANONYMOUS_LOGIN_KEY);
            return true;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null
                && Boolean.TRUE.equals(session.getAttribute(BLOCK_ANONYMOUS_LOGIN_KEY))) {
            // next logout will clear the session anyway !!
            // session.setAttribute(BLOCK_ANONYMOUS_LOGIN_KEY, false);
            return true;
        }
        return false;
    }

    public void initPlugin(Map<String, String> parameters) {
        // NOP
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return Boolean.FALSE;
    }

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return null;
    }

    public Boolean handleLogout(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return Boolean.FALSE;
    }

}
