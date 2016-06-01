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
 *     ldoguin
 */
package org.nuxeo.ecm.webapp.exceptionhandling;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.DefaultNuxeoExceptionHandler;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.runtime.api.Framework;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.*;

/**
 * This exception handler adds security error flag in the URL parameters to ensure the anonymous user will get
 * appropriate error message when being redirected to login page.
 * <p>
 * If it isn't a security exception, or if the user is not anonymous, this handler ends up using
 * DefaultNuxeoExceptionHandler.
 *
 * @author ldoguin
 */
public class NuxeoSecurityExceptionHandler extends DefaultNuxeoExceptionHandler {

    private static final Log log = LogFactory.getLog(NuxeoSecurityExceptionHandler.class);

    protected PluggableAuthenticationService service;

    @Override
    public void handleException(HttpServletRequest request, HttpServletResponse response, Throwable t)
            throws IOException, ServletException {

        Throwable unwrappedException = unwrapException(t);
        if (!ExceptionHelper.isSecurityError(unwrappedException)) {
            super.handleException(request, response, t);
            return;
        }

        Principal principal = request.getUserPrincipal();
        if (principal instanceof NuxeoPrincipal) {
            NuxeoPrincipal nuxeoPrincipal = (NuxeoPrincipal) principal;
            if (nuxeoPrincipal.isAnonymous()) {
                // redirect to login than to requested page
                if (handleAnonymousException(request, response)) {
                    return;
                }
            }
        }
        // go back to default handler
        super.handleException(request, response, t);
    }

    /**
     * Handles the Security Error when the user is anonymous.
     *
     * @return {@code true} if the Security Error is handled so that the calling method won't fallback on the default
     *         handler, {@code false} otherwise.
     */
    protected boolean handleAnonymousException(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        getAuthenticationService().invalidateSession(request);
        Map<String, String> urlParameters = new HashMap<String, String>();
        urlParameters.put(SECURITY_ERROR, "true");
        urlParameters.put(FORCE_ANONYMOUS_LOGIN, "true");
        if (request.getAttribute(REQUESTED_URL) != null) {
            urlParameters.put(REQUESTED_URL, (String) request.getAttribute(REQUESTED_URL));
        } else {
            urlParameters.put(REQUESTED_URL, NuxeoAuthenticationFilter.getRequestedUrl(request));
        }
        // Redirect to login with urlParameters
        if (!response.isCommitted()) {
            String baseURL = getAuthenticationService().getBaseURL(request) + LOGOUT_PAGE;
            request.setAttribute(DISABLE_REDIRECT_REQUEST_KEY, true);
            baseURL = URIUtils.addParametersToURIQuery(baseURL, urlParameters);
            response.sendRedirect(baseURL);
            responseComplete();
        } else {
            log.error("Cannot redirect to login page: response is already committed");
        }
        return true;
    }

    @Override
    protected void responseComplete() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.responseComplete();
        } else {
            log.error("Cannot set response complete: faces context is null");
        }
    }

    protected PluggableAuthenticationService getAuthenticationService() throws ServletException {
        if (service != null) {
            return service;
        }
        service = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                PluggableAuthenticationService.NAME);
        if (service == null) {
            throw new ServletException("Can't initialize Nuxeo Pluggable Authentication Service: "
                    + PluggableAuthenticationService.NAME);
        }
        return service;
    }

}
