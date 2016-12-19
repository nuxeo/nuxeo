/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.SSO_INITIAL_URL_REQUEST_KEY;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.DefaultNuxeoExceptionHandler;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.runtime.api.Framework;

public class SecurityExceptionHandler extends DefaultNuxeoExceptionHandler {

    public static final String CAS_REDIRECTION_URL = "/cas2.jsp";

    public static final String COOKIE_NAME_LOGOUT_URL = "cookie.name.logout.url";

    Cas2Authenticator cas2Authenticator;

    public SecurityExceptionHandler() {
    }

    @Override
    public void handleException(HttpServletRequest request, HttpServletResponse response, Throwable t)
            throws IOException, ServletException {

        if (response.containsHeader("Cache-Control")) {
            response.setHeader("Cache-Control", "no-cache");
        }

        Throwable unwrappedException = ExceptionHelper.unwrapException(t);

        if (!ExceptionHelper.isSecurityError(unwrappedException)
                && !response.containsHeader(SSO_INITIAL_URL_REQUEST_KEY)) {
            super.handleException(request, response, t);
            return;
        }

        Principal principal = request.getUserPrincipal();
        NuxeoPrincipal nuxeoPrincipal;
        if (principal instanceof NuxeoPrincipal) {
            nuxeoPrincipal = (NuxeoPrincipal) principal;
            // redirect to login than to requested page
            if (nuxeoPrincipal.isAnonymous()) {
                response.resetBuffer();

                String urlToReach = getURLToReach(request);
                Cookie cookieUrlToReach = new Cookie(NXAuthConstants.SSO_INITIAL_URL_REQUEST_KEY, urlToReach);
                cookieUrlToReach.setPath("/");
                cookieUrlToReach.setMaxAge(60);
                response.addCookie(cookieUrlToReach);

                if (!response.isCommitted()) {
                    request.getRequestDispatcher(CAS_REDIRECTION_URL).forward(request, response);
                }
                parameters.getListener().responseComplete();
                return;
            }
        }
        // go back to default handler
        super.handleException(request, response, t);
    }

    protected Cas2Authenticator getCasAuthenticator() {
        if (cas2Authenticator != null) {
            return cas2Authenticator;
        }

        PluggableAuthenticationService service = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                PluggableAuthenticationService.NAME);
        if (service == null) {
            throw new NuxeoException("Can't initialize Nuxeo Pluggable Authentication Service");
        }

        cas2Authenticator = (Cas2Authenticator) service.getPlugin("CAS2_AUTH");

        if (cas2Authenticator == null) {
            throw new NuxeoException("Can't get CAS authenticator");
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
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }

    protected URLPolicyService getURLPolicyService() {
        return Framework.getService(URLPolicyService.class);
    }

}
