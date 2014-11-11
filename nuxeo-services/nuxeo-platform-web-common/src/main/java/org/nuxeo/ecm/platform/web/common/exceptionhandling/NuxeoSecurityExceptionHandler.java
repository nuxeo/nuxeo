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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.web.common.exceptionhandling;

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
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * This exception handler adds security error flag in the URL parameters
 * to ensure the anonymous user will get appropriate error message when being redirected
 * to login page.
 *
 * If it isn't a security exception, or if the user is not anonymous, this handler
 * ends up using DefaultNuxeoExceptionHandler.
 *
 * @author ldoguin
 *
 */
public class NuxeoSecurityExceptionHandler extends DefaultNuxeoExceptionHandler {

    protected static final Log log = LogFactory.getLog(NuxeoSecurityExceptionHandler.class);

    private PluggableAuthenticationService service;

    public NuxeoSecurityExceptionHandler() throws Exception {
        // initAuthentificationService();
    }

    @Override
    public void handleException(HttpServletRequest request,
            HttpServletResponse response, Throwable t) throws IOException,
            ServletException {

        Throwable unwrappedException = unwrapException(t);
        if (!ExceptionHelper.isSecurityError(unwrappedException)) {
            super.handleException(request, response, t);
            return;
        }

        Map<String, String> urlParameters = new HashMap<String, String>();
        Principal principal = request.getUserPrincipal();
        NuxeoPrincipal nuxeoPrincipal = null;
        if (principal instanceof NuxeoPrincipal) {
            nuxeoPrincipal = (NuxeoPrincipal) principal;
            // redirect to login than to requested page
            if (nuxeoPrincipal.isAnonymous()) {
                urlParameters.put(NXAuthConstants.SECURITY_ERROR, "true");
                urlParameters.put(NXAuthConstants.FORCE_ANONYMOUS_LOGIN, "true");
                urlParameters.put(NXAuthConstants.REQUESTED_URL,
                        NuxeoAuthenticationFilter.getRequestedUrl(request));
                // Redirect to login with urlParameters
                if (!response.isCommitted()) {
                    String baseURL = initAuthentificationService().getBaseURL(
                            request)
                            + NXAuthConstants.LOGOUT_PAGE;
                    request.setAttribute(
                            NuxeoAuthenticationFilter.URLPolicyService_DISABLE_REDIRECT_REQUEST_KEY,
                            true);
                    baseURL = URIUtils.addParametersToURIQuery(baseURL,
                            urlParameters);
                    response.sendRedirect(baseURL);
                    FacesContext.getCurrentInstance().responseComplete();
                }
                return;
            }
        }
        // go back to default handler
        super.handleException(request, response, t);
    }

    private PluggableAuthenticationService initAuthentificationService()
            throws ServletException {
        service = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                PluggableAuthenticationService.NAME);
        if (service == null) {
            log.error("Unable to get Service "
                    + PluggableAuthenticationService.NAME);
            throw new ServletException(
                    "Can't initialize Nuxeo Pluggable Authentication Service");
        }
        return service;
    }
}
