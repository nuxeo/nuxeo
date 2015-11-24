/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.DefaultNuxeoExceptionHandler;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.runtime.api.Framework;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.SSO_INITIAL_URL_REQUEST_KEY;

public class SecurityExceptionHandler extends DefaultNuxeoExceptionHandler {

    public static final String CAS_REDIRECTION_URL = "/cas2.jsp";

    public static final String COOKIE_NAME_LOGOUT_URL = "cookie.name.logout.url";

    private static final Log log = LogFactory.getLog(SecurityExceptionHandler.class);

    Cas2Authenticator cas2Authenticator;

    protected URLPolicyService urlService;

    public SecurityExceptionHandler() throws Exception {
    }

    @Override
    public void handleException(HttpServletRequest request,
            HttpServletResponse response, Throwable t) throws IOException,
            ServletException {

        if (response.containsHeader("Cache-Control")) {
            response.setHeader("Cache-Control", "no-cache");
        }

        Throwable unwrappedException = unwrapException(t);

        if (!ExceptionHelper.isSecurityError(unwrappedException)
                && !response.containsHeader(SSO_INITIAL_URL_REQUEST_KEY)) {
            super.handleException(request, response, t);
            return;
        }

        Principal principal = request.getUserPrincipal();
        NuxeoPrincipal nuxeoPrincipal = null;
        if (principal instanceof NuxeoPrincipal) {
            nuxeoPrincipal = (NuxeoPrincipal) principal;
            // redirect to login than to requested page
            if (nuxeoPrincipal.isAnonymous()) {
                response.resetBuffer();

                String urlToReach = getURLToReach(request);
                Cookie cookieUrlToReach = new Cookie(
                        NXAuthConstants.SSO_INITIAL_URL_REQUEST_KEY, urlToReach);
                cookieUrlToReach.setPath("/");
                cookieUrlToReach.setMaxAge(60);
                response.addCookie(cookieUrlToReach);

                if (!response.isCommitted()) {
                    request.getRequestDispatcher(CAS_REDIRECTION_URL).forward(request,
                            response);
                }
                FacesContext.getCurrentInstance().responseComplete();
                return;
            }
        }
        // go back to default handler
        super.handleException(request, response, t);
    }

    protected Cas2Authenticator getCasAuthenticator() throws ClientException {
        if (cas2Authenticator != null) {
            return cas2Authenticator;
        }

        PluggableAuthenticationService service = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                PluggableAuthenticationService.NAME);
        if (service == null) {
            throw new ClientException(
                    "Can't initialize Nuxeo Pluggable Authentication Service");
        }

        cas2Authenticator = (Cas2Authenticator) service.getPlugin("CAS2_AUTH");

        if (cas2Authenticator == null) {
            throw new ClientException("Can't get CAS authenticator");
        }
        return cas2Authenticator;
    }

    protected String getURLToReach(HttpServletRequest request) {
        DocumentView docView = (DocumentView) request.getAttribute(URLPolicyService.DOCUMENT_VIEW_REQUEST_KEY);

        if (docView != null) {
            String urlToReach = getURLPolicyService().getUrlFromDocumentView(docView, "");

            if (urlToReach != null) {
                return urlToReach;
            }
        }
        return request.getRequestURL().toString() + "?"
                + request.getQueryString();
    }

    protected URLPolicyService getURLPolicyService() {
        if (urlService == null) {
            try {
                urlService = Framework.getService(URLPolicyService.class);
            } catch (Exception e) {
                log.error("Could not retrieve the URLPolicyService", e);
            }
        }
        return urlService;
    }

}
