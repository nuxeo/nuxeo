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
 *     Nuxeo - initial API and implementation
 *     Academie de Rennes - proxy CAS support
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.AnonymousAuthenticator;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Anonymous authenticator that redirect logout to CAS server authentication to
 * connect to nuxeo.
 *
 * @author Benjamin JALON
 */
public class AnonymousAuthenticatorForCAS2 extends AnonymousAuthenticator {

    protected static final Log log = LogFactory.getLog(AnonymousAuthenticatorForCAS2.class);

    @Override
    public Boolean handleLogout(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        boolean isRedirectionToCas = false;

        Cookie[] cookies = httpRequest.getCookies();
        for (Cookie cookie : cookies) {
            if (NXAuthConstants.SSO_INITIAL_URL_REQUEST_KEY.equals(cookie.getName())) {
                isRedirectionToCas = true;
                break;
            }
        }

        if (isRedirectionToCas) {
            String authURL = getCas2Authenticator().getServiceURL(httpRequest,
                    Cas2Authenticator.LOGIN_ACTION);
            String appURL = getCas2Authenticator().getAppURL(httpRequest);
            String urlToReach = authURL + "?service=" + appURL;

            try {
                httpResponse.sendRedirect(urlToReach);
                return true;
            } catch (IOException e) {
                log.error("Unable to redirect to CAS logout screen:", e);
                return false;
            }
        }

        return super.handleLogout(httpRequest, httpResponse);
    }

    protected Cas2Authenticator casAuthenticator;

    public Cas2Authenticator getCas2Authenticator() {

        if (casAuthenticator == null) {
            PluggableAuthenticationService service = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                    PluggableAuthenticationService.NAME);

            if (service == null) {
                log.error("Can't get PluggableAuthenticationService");
            }

            NuxeoAuthenticationPlugin plugin = service.getPlugin("CAS2_AUTH");

            if (plugin == null) {
                log.error("Can't get Cas Authenticator from PluggableAuthenticationService");
            }
            casAuthenticator = (Cas2Authenticator) plugin;
        }

        return casAuthenticator;
    }

}
