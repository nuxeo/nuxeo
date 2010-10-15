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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.shibboleth.auth.exceptionhandling;

import java.io.IOException;
import java.net.URLEncoder;

import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.web.Session;
import org.nuxeo.ecm.platform.shibboleth.service.ShibbolethAuthenticationConfig;
import org.nuxeo.ecm.platform.shibboleth.service.ShibbolethAuthenticationService;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoSecurityExceptionHandler;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class ShibbolethSecurityExceptionHandler extends
        NuxeoSecurityExceptionHandler {

    protected ShibbolethAuthenticationConfig config;

    @Override
    protected boolean handleAnonymousException(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        ShibbolethAuthenticationConfig config = getConfig();
        if (config != null) {
            String loginURL = config.getLoginURL();
            try {
                if (loginURL == null) {
                    log.error("Unable to handle Shibboleth login, no loginURL registered");
                }
                String redirectURL = VirtualHostHelper.getBaseURL(request);
                if (request.getAttribute(NXAuthConstants.REQUESTED_URL) != null) {
                    redirectURL += request.getAttribute(NXAuthConstants.REQUESTED_URL);
                }
                redirectURL = URLEncoder.encode(redirectURL, "UTF-8");

                loginURL = loginURL + "?target=" + redirectURL;
                if (!response.isCommitted()) {
                    request.setAttribute(
                            NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, true);
                    Session.instance().invalidate();
                    response.sendRedirect(loginURL);
                    FacesContext fContext = FacesContext.getCurrentInstance();
                    if (fContext != null) {
                        fContext.responseComplete();
                    } else {
                        log.error("Cannot set response complete: faces context is null");
                    }
                } else {
                    log.error("Cannot redirect to login page: response is already commited");
                }
            } catch (IOException e) {
                String errorMessage = String.format(
                        "Unable to handle Shibboleth login on %s", loginURL);
                log.error(errorMessage, e);
            }
            return true;
        }
        return false;
    }

    protected ShibbolethAuthenticationConfig getConfig() {
        if (config == null) {
            try {
                ShibbolethAuthenticationService service = Framework.getService(ShibbolethAuthenticationService.class);
                config = service.getConfig();
            } catch (Exception e) {
                log.error(
                        "Failed to load Shibboleth authentication configuration",
                        e);
            }
        }
        return config;
    }

}
