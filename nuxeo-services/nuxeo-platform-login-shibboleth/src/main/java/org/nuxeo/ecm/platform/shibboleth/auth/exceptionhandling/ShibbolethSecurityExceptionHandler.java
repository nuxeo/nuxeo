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

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.USERIDENT_KEY;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.web.Session;
import org.nuxeo.ecm.platform.shibboleth.service.ShibbolethAuthenticationService;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.NuxeoSecurityExceptionHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class ShibbolethSecurityExceptionHandler extends NuxeoSecurityExceptionHandler {

    private static final Log log = LogFactory.getLog(ShibbolethSecurityExceptionHandler.class);

    @Override
    protected boolean handleAnonymousException(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (getService() == null) {
            return false;
        }
        String loginURL = getService().getLoginURL(request);
        if (loginURL == null) {
            log.error("Unable to handle Shibboleth login, no loginURL registered");
            return false;
        }
        try {
            if (!response.isCommitted()) {
                request.setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, true);
                response.sendRedirect(loginURL);
                if (Contexts.isSessionContextActive()) {
                    Session.instance().invalidate();
                    FacesContext fContext = FacesContext.getCurrentInstance();
                    if (fContext != null) {
                        fContext.responseComplete();
                    } else {
                        log.error("Cannot set response complete: faces context is null");
                    }
                }
            } else {
                log.error("Cannot redirect to login page: response is already commited");
            }
        } catch (IOException e) {
            String errorMessage = String.format("Unable to handle Shibboleth login on %s", loginURL);
            log.error(errorMessage, e);
        }
        return true;
    }

    protected ShibbolethAuthenticationService getService() {
        return Framework.getService(ShibbolethAuthenticationService.class);
    }

    @Override
    protected Principal getPrincipal(HttpServletRequest request) {
        Principal principal = super.getPrincipal(request);
        if (principal == null) {
            principal = Optional.ofNullable(request.getSession(false))
                                .map(s -> (CachableUserIdentificationInfo) s.getAttribute(USERIDENT_KEY))
                                .map(CachableUserIdentificationInfo::getPrincipal)
                                .orElse(null);
        }
        return principal;
    }

}
