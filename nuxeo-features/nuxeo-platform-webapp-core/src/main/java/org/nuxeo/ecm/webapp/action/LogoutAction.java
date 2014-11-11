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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.action;

import java.io.IOException;
import java.io.Serializable;

import javax.ejb.Remove;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthContants;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.webapp.base.InputController;

/**
 * Logs the user in/out.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */

@Name("loginLogoutAction")
@Scope(ScopeType.STATELESS)
// @Startup
public class LogoutAction extends InputController implements Serializable {

    private static final long serialVersionUID = -5100044672151458204L;

    private static final Log log = LogFactory.getLog(LogoutAction.class);

    public String login() {
        return navigationContext.goHome();
    }

    /**
     * Logs the user out. Invalidates the HTTP session so that it cannot be used
     * anymore.
     *
     * @return the next page that is going to be displayed
     * @throws IOException
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

        if (response != null && request != null && !context.getResponseComplete()) {
            String baseURL = BaseURL.getBaseURL(request);
            request.setAttribute(URLPolicyService.DISABLE_REDIRECT_REQUEST_KEY,
                    true);
            response.sendRedirect(baseURL + NXAuthContants.LOGOUT_PAGE);
            context.responseComplete();
        }
        return null;
    }

    @Destroy
    @Remove
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

}
