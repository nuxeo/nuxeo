/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.webapp;

import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGOUT_PAGE;

import java.io.IOException;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("logoutActions")
@Scope(ScopeType.STATELESS)
@Install(precedence = FRAMEWORK)
public class LogoutActions {

    /**
     * Logs the user out. Invalidates the HTTP session so that it cannot be used
     * anymore.
     *
     * @return the next page that is going to be displayed
     */
    public static String logout() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        Object req = eContext.getRequest();
        Object resp = eContext.getResponse();
        HttpServletRequest request = null;
        if (req instanceof HttpServletRequest) {
            request = (HttpServletRequest) req;
        }
        HttpServletResponse response = null;
        if (resp instanceof HttpServletResponse) {
            response = (HttpServletResponse) resp;
        }

        if (response != null && request != null
                && !context.getResponseComplete()) {
            String baseURL = BaseURL.getBaseURL(request);
            request.setAttribute(DISABLE_REDIRECT_REQUEST_KEY, true);
            response.sendRedirect(baseURL + LOGOUT_PAGE);
            context.responseComplete();
        }
        return null;
    }

}
